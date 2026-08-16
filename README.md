# CareerPilot Backend

> AI-powered career preparation platform built with Java, Spring Boot, PostgreSQL, PGVector, and Spring AI.

CareerPilot is a backend platform designed to help users prepare for software engineering careers through **AI-powered resume analysis, coding practice, interview preparation, profile management, and career-readiness tracking**.

The backend exposes REST APIs consumed by the CareerPilot frontend and integrates AI capabilities for resume analysis and interview preparation.

---

## 🚀 Key Features

### 🔐 Authentication

* User registration and login
* JWT-based authentication
* Secure password handling
* Protected REST endpoints

### 📄 AI Resume Analysis

* Resume upload and PDF processing
* Resume content extraction
* AI-powered resume analysis
* Role-specific resume evaluation
* Resume history and analysis results
* Resume quality insights and recommendations

### 💻 Coding Practice

* DSA topics and subtopics
* Coding question catalog
* Difficulty-based filtering
* Company-based filtering
* Platform information
* Search and pagination
* Track solved questions
* Coding progress metrics

### 🎯 AI Interview Preparation

* Start interview sessions
* Generate interview questions
* Support different interview types
* Submit answers for evaluation
* AI-powered answer evaluation
* Follow-up questions
* Interview reports and performance analysis
* Interview history

### 📊 Career Dashboard

* Career-readiness metrics
* Coding activity
* Skill analysis
* Topic distribution
* Weekly activity
* Readiness trends
* Upcoming tasks
* Leaderboard information

### 👤 Profile Management

* User profile
* Skills
* Education
* Experience
* Bio and career information

### 🧠 AI & Knowledge Retrieval

* Spring AI integration
* Ollama-based AI processing
* PostgreSQL PGVector support
* Knowledge/document storage
* Vector-based context retrieval

---

## 🏗️ Architecture

```text
                         CareerPilot Frontend
                                  |
                                  | REST API
                                  v
                    ┌─────────────────────────┐
                    │    Spring Boot API      │
                    └────────────┬────────────┘
                                 |
             ┌───────────────────┼───────────────────┐
             |                   |                   |
             v                   v                   v
       Authentication       Core Features          AI Layer
             |                   |                   |
          JWT              Resume Analysis      Spring AI
                           Coding Practice        Ollama
                           Interviews             RAG
                           Dashboard
                           Profile
             |                   |                   |
             └───────────────────┼───────────────────┘
                                 |
                                 v
                    ┌─────────────────────────┐
                    │       PostgreSQL        │
                    │                         │
                    │      + PGVector         │
                    └─────────────────────────┘
```

---

## 🛠️ Technology Stack

### Backend

* Java 21
* Spring Boot 3.5.5
* Spring Web
* Spring Data JPA
* Spring Validation

### Database

* PostgreSQL
* PGVector

### AI

* Spring AI
* Ollama
* Retrieval-Augmented Generation (RAG)
* Vector similarity search

### Security

* JWT Authentication
* Password hashing
* Protected REST APIs

### Document Processing

* Apache PDFBox

### Build & Deployment

* Maven
* Docker
* Docker Compose

---

## 📁 Project Structure

```text
src/main/java/com/example/careerpilot
│
├── config/
│   ├── AiConfig.java
│   └── JacksonConfig.java
│
├── controller/
│   ├── AuthController.java
│   ├── CodingController.java
│   ├── DashboardController.java
│   ├── InterviewController.java
│   └── ResumeController.java
│
├── dto/
│   ├── dashboard/
│   └── ...
│
├── exception/
│   └── ...
│
├── mapper/
│   └── DashboardMapper.java
│
├── model/
│   ├── CodingQuestion.java
│   ├── CodingTopic.java
│   ├── InterviewAttempt.java
│   ├── InterviewQuestion.java
│   ├── InterviewSession.java
│   ├── KnowledgeDocument.java
│   ├── Resume.java
│   ├── Skill.java
│   └── User.java
│
├── repository/
│   └── ...
│
├── service/
│   └── ...
│
└── CareerpilotApplication.java
```

---

## 🔌 Main API Modules

| Module         | Base Path            | Purpose                       |
| -------------- | -------------------- | ----------------------------- |
| Authentication | `/api/v1/auth`       | Registration and login        |
| Coding         | `/api/v1/coding`     | DSA questions and progress    |
| Dashboard      | `/api/v1/dashboard`  | Career readiness and activity |
| Interviews     | `/api/v1/interviews` | AI interview preparation      |
| Resumes        | `/api/v1/resumes`    | Resume upload and AI analysis |

---

## 🤖 AI Workflow

CareerPilot uses AI to provide personalized career preparation.

### Resume Analysis

```text
Resume PDF
    ↓
PDF Text Extraction
    ↓
Resume Processing
    ↓
Role Context
    ↓
AI Analysis
    ↓
Skills / Quality / Recommendations
    ↓
Stored Analysis
```

### AI Interview

```text
User starts interview
        ↓
Interview configuration
        ↓
AI question generation
        ↓
User submits answer
        ↓
AI evaluation
        ↓
Feedback + scoring
        ↓
Follow-up question
        ↓
Interview report
```

---

## 🗄️ Database

The application uses **PostgreSQL** for persistent application data and **PGVector** for vector-based AI knowledge retrieval.

Major domain models include:

* User
* Resume
* Coding Question
* Coding Topic
* Interview Session
* Interview Question
* Interview Attempt
* Skill
* Education
* Experience
* Knowledge Document
* Company Knowledge

---

## ⚙️ Getting Started

### Prerequisites

Make sure you have:

* Java 21
* Maven
* PostgreSQL
* Docker Desktop
* Ollama

### 1. Clone the repository

```bash
git clone https://github.com/Satyapranay1/CareerPilot---backend.git
cd CareerPilot---backend
```

### 2. Configure environment variables

Create the required environment variables for your local environment.

Example:

```text
DB_URL=jdbc:postgresql://localhost:5432/careerpilot
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_secret
```

Do **not** commit credentials or secrets to GitHub.

### 3. Start PostgreSQL / PGVector

The repository includes Docker Compose configuration.

```bash
docker compose up -d
```

### 4. Start Ollama

Make sure Ollama is installed and the required model is available locally.

```bash
ollama serve
```

### 5. Run the application

Using Maven:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API will be available at:

```text
http://localhost:8080
```

---

## 🧪 Testing

Run the test suite with:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

---

## 🔒 Security

Sensitive configuration is managed through environment variables rather than being committed directly to the repository.

Never commit:

* Database passwords
* JWT secrets
* API keys
* Personal credentials
* Local environment files

---

## 📌 Future Improvements

Potential improvements include:

* Production cloud deployment
* Improved AI evaluation models
* Advanced career recommendations
* More coding platforms and question sources
* Enhanced analytics
* Automated career roadmaps
* Additional AI-powered career insights

---

## 👨‍💻 Author

**Kotha Venkata Satya Pranay**

Full-Stack Developer | Java | Spring Boot | React | PostgreSQL | AI/ML

* GitHub: https://github.com/Satyapranay1
* Portfolio: https://pranay-portfolio-one.vercel.app
* Email: [satyapranay114@gmail.com](mailto:satyapranay114@gmail.com)

---

⭐ If you find this project useful, consider giving the repository a star.
