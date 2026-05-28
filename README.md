# CarDekho AI Matchmaker

An AI-native car recommendation dashboard built for the modern auto buyer.

CarDekho AI Matchmaker allows users to simply input their constraints (budget, seating, habits) and priorities (Safety, Mileage, Performance). It queries a local SQL database of cars, truncates the dataset into a strict context window, and prompts Groq's high-speed **Llama-3.3-70b-versatile** model to construct a personalized, well-reasoned shortlist of vehicles.

<p align="center">
  <img src="docs/1_landing.png" alt="Landing Page" width="800"/>
  <br/>
  <i>Role Selection Dashboard</i>
</p>

## ✨ Highlights

### AI Matchmaker
Get detailed, personalized LLM reasoning weighing trade-offs for your specific constraints.
<p align="center">
  <img src="docs/6_matchmaker_input.png" alt="Matchmaker Input" width="800"/>
</p>
<p align="center">
  <img src="docs/4_matchmaker_results.png" alt="Matchmaker Results" width="800"/>
</p>

### Dealer Dataset Management
Upload new spreadsheet catalogs dynamically or batch-delete outdated inventory.
<p align="center">
  <img src="docs/2_dealer_manager.png" alt="Dealer Manager" width="800"/>
</p>
<p align="center">
  <img src="docs/3_dealer_table.png" alt="Data Table" width="800"/>
</p>

### Browse Catalog
Explore the entire generated and parsed database dynamically.
<p align="center">
  <img src="docs/5_browse_catalog.png" alt="Browse Catalog" width="800"/>
</p>

The absolute easiest way to run the entire stack locally is by using Docker Compose.

1. Ensure you have Docker installed and running.
2. Clone this repository and navigate to the project root.
3. Run the following command:

```bash
docker-compose up --build
```

The system will build both the Spring Boot backend and the React frontend. Once it says the containers are running:
- Open your browser to **http://localhost:5173** to access the UI!
- The backend Swagger UI can be found at **http://localhost:8080/swagger-ui/index.html**

## 🔑 AI Matchmaking Setup (Groq API Key)

To use the AI Matchmaker, you need a free Groq API key.
1. Go to [console.groq.com](https://console.groq.com/) and simply **Login with Google**.
2. Generate an API Key (it will start with `gsk_`).
3. *Note: Groq offers an extremely generous **free tier** that is more than enough for testing and personal use, so you won't need to enter any payment details!*

### Option A: Set it in the UI (Easiest)
Once you launch the app, click **"I am a Dealer"**. In the Dataset Manager, you'll find a field to enter your **Temporary API Key**. This will save the key to your browser's local storage and use it for matchmaking requests.
*Note: This UI field is primarily a temporary convenience for rapid development and testing without having to restart the backend container.*

### Option B: Set it via Environment Variable
If you want a more permanent setup so that you don't have to use the UI, you can supply it as an environment variable before running Docker Compose:

**Windows (PowerShell):**
```powershell
$env:GROQ_API_KEY="gsk_your_key_here"
docker-compose up --build
```
**Mac/Linux:**
```bash
GROQ_API_KEY="gsk_your_key_here" docker-compose up --build
```

## 🛠 Features

- **Semantic Matchmaking**: Ask for cars prioritizing "Safety and Cabin Tech", and the AI understands the nuance beyond just filtering.
- **Dataset Manager**: Upload `.xlsx` spreadsheets natively to expand the car database on the fly.
- **Multi-select Deletion**: Easily prune old inventory in the Dealer dashboard.
- **Spring AI Integration**: Utilizes Spring's official AI library for robust chat client interactions and automated retries.

## 📦 Tech Stack
- **Backend**: Java 21, Spring Boot 3.x, Spring AI, H2 Database
- **Frontend**: React 18, Vite, Lucide Icons, Vanilla CSS Glassmorphism
