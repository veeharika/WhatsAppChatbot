package com.example.demo.service;

import com.example.demo.entity.Patient;
import com.example.demo.repository.PatientRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BotService {

    private static final Logger LOGGER = Logger.getLogger(BotService.class.getName());

    @Autowired
    private PatientRepository patientRepository;

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.api.token}")
    private String apiToken;

	@Value("${whatsapp.phone.number}")
    private String recipientPhoneNumber;

    @Value("${whatsapp.webhook.verify_token}")
    private String webhookVerifyToken;

    private static final String ENCRYPTION_KEY = "ThisIsASecretKey"; // Use a secure key management system in production

    private Patient currentPatient; // Store current patient data temporarily
    private String currentState = "waitingForName"; // Manage conversation state

    public void processWebhook(String payload) {
        String message = extractMessageFromPayload(payload);
        if (message != null && !message.trim().isEmpty()) {
            processMessage(message);
        }
    }

    public boolean verifyWebhook(String mode, String token) {
        return "subscribe".equals(mode) && webhookVerifyToken.equals(token);
    }

    private String extractMessageFromPayload(String payload) {
        try {
            JSONObject jsonPayload = new JSONObject(payload);
            JSONArray entry = jsonPayload.getJSONArray("entry");
            JSONObject changes = entry.getJSONObject(0).getJSONArray("changes").getJSONObject(0);
            JSONObject value = changes.getJSONObject("value");
            JSONArray messages = value.getJSONArray("messages");
            JSONObject messageObj = messages.getJSONObject(0);
            return messageObj.getJSONObject("text").getString("body");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing payload", e);
            return null; // Return null if parsing fails
        }
    }

    private void processMessage(String message) {
        String responseMessage;
		String lowerCaseMessage = message.toLowerCase().trim();

		// If the bot is in the initial state, and the user sends a greeting, start the conversation
		if (currentState.equals("waitingForName") && (lowerCaseMessage.equals("hi") || lowerCaseMessage.equals("hello"))) {
			responseMessage = "Hello! Please provide your name.";
		} else {
        switch (currentState) {
            case "waitingForName":
                currentPatient = new Patient(); // Initialize new patient object
                currentPatient.setName(message.trim());
                if (currentPatient.getName().isEmpty()) {
                    responseMessage = "Please provide a valid name.";
                    break; 
                }
                responseMessage = "Thank you, " + currentPatient.getName() + ". Please provide your date of birth (e.g., DD/MM/YYYY).";
                currentState = "waitingForDOB";
                break; 
            case "waitingForDOB":
                currentPatient.setDateOfBirth(message.trim());
                if (currentPatient.getDateOfBirth().isEmpty()) {
                    responseMessage = "Please provide a valid date of birth.";
                    break; 
                }
                responseMessage = "Please provide your gender (e.g., Male, Female, Other).";
                currentState = "waitingForGender";
                break; 
            case "waitingForGender":
                currentPatient.setGender(message.trim());
                if (currentPatient.getGender().isEmpty()) {
                    responseMessage = "Please provide a valid gender.";
                    break; 
                }
                responseMessage = "Please provide your address.";
                currentState = "waitingForAddress";
                break; 
            case "waitingForAddress":
                currentPatient.setAddress(message.trim());
                if (currentPatient.getAddress().isEmpty()) {
                    responseMessage = "Please provide a valid address.";
                    break; 
                }
                responseMessage = "Please provide your medical history (e.g., allergies, previous surgeries).";
                currentState = "waitingForMedicalHistory";
                break; 
            case "waitingForMedicalHistory":
                currentPatient.setMedicalHistory(message.trim());
                if (currentPatient.getMedicalHistory().isEmpty()) {
                    responseMessage = "Please provide valid medical history.";
                    break; 
                }
                responseMessage = "Please list any current medications you're taking.";
                currentState = "waitingForMedications";
                break; 
            case "waitingForMedications":
                currentPatient.setMedications(message.trim());
                if (currentPatient.getMedications().isEmpty()) {
                    responseMessage = "Please provide valid medication details.";
                    break; 
                }
                responseMessage = "Thank you for providing your information. Your details are now saved.";
                savePatientData(); // Save patient data to the database
                currentState = "waitingForName"; // Reset conversation after saving data
                break; 
            default:
                responseMessage = "Sorry, I couldn't understand your message. Please try again.";
                currentState = "waitingForName"; // Reset conversation if the state is unknown
                break; 
        }
	}
        sendWhatsAppMessage(responseMessage); // Send response back to WhatsApp user
    }

   public void sendWhatsAppMessage(String message) {
        RestTemplate restTemplate = new RestTemplate();
        String payload = "{"
                + "\"messaging_product\": \"whatsapp\","
                + "\"to\": \"" + recipientPhoneNumber + "\","
                + "\"text\": {"
                + "\"body\": \"" + message + "\""
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            System.out.println("Sending payload: " + payload);
            System.out.println("Headers: " + headers);
            String response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class).getBody();
            System.out.println("Response from WhatsApp API: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     @Transactional
     private void savePatientData() { // Save patient data securely in the database.
         try { 
             currentPatient.setName(encrypt(currentPatient.getName())); 
             currentPatient.setDateOfBirth(encrypt(currentPatient.getDateOfBirth())); 
             currentPatient.setAddress(encrypt(currentPatient.getAddress())); 
             currentPatient.setMedicalHistory(encrypt(currentPatient.getMedicalHistory())); 
             currentPatient.setMedications(encrypt(currentPatient.getMedications())); 

             patientRepository.save(currentPatient); // Save encrypted patient data in the database.
             LOGGER.info("Patient data saved successfully."); 
         } catch (Exception e) { 
             LOGGER.log(Level.SEVERE, "Error saving patient data", e); 
         } 
     }

     public void exportToCSV(String filePath) throws IOException { // Export patient data to CSV file.
         List<Patient> patients = getAllPatients(); 

         try (FileOutputStream fos = new FileOutputStream(filePath)) { 
             StringBuilder sb = new StringBuilder(); 
             sb.append("ID,Name,Date of Birth,Gender,Address,Medical History,Medications\n"); 

             for (Patient patient : patients) { 
                 sb.append(String.format("%d,%s,%s,%s,%s,%s,%s\n", patient.getId(), patient.getName(), patient.getDateOfBirth(), patient.getGender(), patient.getAddress(), patient.getMedicalHistory(), patient.getMedications())); 
             } 

             fos.write(sb.toString().getBytes()); // Write CSV content to file.
         } 
     }

     public void exportToExcel(String filePath) throws IOException { // Export patient data to Excel file.
         List<Patient> patients = getAllPatients(); 

         try (Workbook workbook = new XSSFWorkbook()) { 
             Sheet sheet = workbook.createSheet("Patients"); 

             Row headerRow = sheet.createRow(0); 
             headerRow.createCell(0).setCellValue("ID"); 
             headerRow.createCell(1).setCellValue("Name"); 
             headerRow.createCell(2).setCellValue("Date of Birth"); 
             headerRow.createCell(3).setCellValue("Gender"); 
             headerRow.createCell(4).setCellValue("Address"); 
             headerRow.createCell(5).setCellValue("Medical History"); 
             headerRow.createCell(6).setCellValue("Medications");

             int rowNum = 1; 

             for (Patient patient : patients) { 
                 Row row = sheet.createRow(rowNum++); 

                 row.createCell(0).setCellValue(patient.getId()); 
                 row.createCell(1).setCellValue(patient.getName()); 
                 row.createCell(2).setCellValue(patient.getDateOfBirth()); 
                 row.createCell(3).setCellValue(patient.getGender()); 
                 row.createCell(4).setCellValue(patient.getAddress()); 
                 row.createCell(5).setCellValue(patient.getMedicalHistory()); 
                 row.createCell(6).setCellValue(patient.getMedications()); 
             } 

             try (FileOutputStream fileOut = new FileOutputStream(filePath)) { 
                 workbook.write(fileOut); // Write Excel content to file.
             } 

         } catch (IOException e) { 
             LOGGER.log(Level.SEVERE, "Error exporting to Excel", e); throw e; // Handle IO exception during export.
         }  
     }

     private List<Patient> getAllPatients() { // Retrieve all patients from the database and decrypt their information.
         List<Patient> patients = patientRepository.findAll(); 

         for (Patient patient : patients) { 
             patient.setName(decrypt(patient.getName())); 
             patient.setDateOfBirth(decrypt(patient.getDateOfBirth())); 
             patient.setAddress(decrypt(patient.getAddress())); 
             patient.setMedicalHistory(decrypt(patient.getMedicalHistory())); 
             patient.setMedications(decrypt(patient.getMedications()));  
         } 

         return patients;  
     }

     private String encrypt(String value) { // Encrypt sensitive data before saving it in the database.
         try {  
              Key key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");  
              Cipher cipher = Cipher.getInstance("AES");  
              cipher.init(Cipher.ENCRYPT_MODE, key);  
              byte[] encryptedBytes= cipher.doFinal(value.getBytes());  
              return Base64.getEncoder().encodeToString(encryptedBytes);  
         } catch (Exception e) {  
              LOGGER.log(Level.SEVERE,"Error encrypting data",e);  
              return value;  // Return original value if encryption fails.
          }  
      }  

      private String decrypt(String encryptedValue) {  // Decrypt sensitive data retrieved from the database.
          try {  
              Key key= new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");  
              Cipher cipher= Cipher.getInstance("AES");  
              cipher.init(Cipher.DECRYPT_MODE,key);  
              byte[] decryptedBytes= cipher.doFinal(Base64.getDecoder().decode(encryptedValue));  
              return new String(decryptedBytes);  
          } catch (Exception e) {  
              LOGGER.log(Level.SEVERE,"Error decrypting data",e);  
              return encryptedValue;  // Return encrypted value if decryption fails.
          }  
      }  

}