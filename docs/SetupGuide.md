
# Bot Setup Guide

## Prerequisites
- WhatsApp Business API access (register at https://www.whatsapp.com/business/api)
- Java 11 or higher
- Maven for dependency management
- Environment Variables for secure configuration

## Project Configuration
1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd project_root
   ```

2. **Set Environment Variables**:
   - `WHATSAPP_API_TOKEN`: API Token for WhatsApp Business.
   -  `API Keys` : Integrate with WhatsApp Business API, following WhatsApp setup instructions for your bot.

3. **Build the Project**:
   ```bash
   mvn clean install
   ```

4. **Run the Bot**:
   ```bash
   mvn spring-boot:run
   ```

## WhatsApp Business API Integration
1. **Set Up Webhooks**:
   - Configure a webhook URL for WhatsApp to deliver messages to the bot. I have added video on how to configure webhooks


## Testing the Application:
   - Use a tool like Postman to test data collection at /api/patients/collect and export at /api/patients/export.


