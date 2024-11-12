# Architecture Overview

## Design
The bot application is structured with a state-based interaction flow, guiding the user through each required piece of information.

- **BotService.java**: Core logic for managing user interactions and the collection of patient data and Handles data encryption using AES to ensure data privacy.
- **Patient.java**: Model representing the data structure for patient information.

## Sequence of Operations
1. **Receive Message**: The bot receives incoming WhatsApp messages via webhook integration.
2. **Process Data**: Based on the conversation state, the bot collects and validates data.
3. **Store and Encrypt**: Patient data is stored in memory and encrypted before saving.
4. **Export**: Data is exported to CSV/Excel format upon authorized request from the provider.

## Data Flow
1. **Data Collection**:
   - Each message is processed and prompts the user for the next data point.
2. **Data Encryption**:
   - Data is encrypted using AES encryption before export.
3. **Data Export**:
   - Authorized personnel can export patient data using a passcode.

## Libraries
- **javax.crypto**: Provides AES encryption.
- **Apache POI (optional)**: Used for exporting data in Excel format if required.
- **WhatsApp Business API**: Manages bot messaging.
