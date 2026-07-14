# 🚀 Jira Clone - Full Stack Project Management System

A full-stack Jira-inspired project management application built to streamline issue tracking, sprint planning, and team collaboration. The application enables users to manage projects, create and assign tasks, track work progress through Kanban boards, upload attachments, manage subtasks, and receive notifications.

## 🌐 Live Demo

**Frontend:** https://jira-clone-anjali.vercel.app

**Backend:** https://jira-clone-backend-rpa6.onrender.com

---

## 📌 Features

### 🔐 Authentication
- User Registration & Login
- Secure Password Validation
- Profile Management
- Change Password
- Email Change Request
- Account Deactivation

### 📂 Project Management
- Create Projects
- Update Project Details
- Delete Projects
- Project Dashboard

### 📋 Issue Management
- Create Issues
- Edit Issues
- Delete Issues
- Assign Issues
- Issue Priorities
- Issue Status Tracking

### 🧩 Subtask Management
- Create Subtasks
- Parent-Child Relationship
- Automatic Project & Sprint Inheritance
- Prevent Parent Completion Until Subtasks Are Complete

### 🏃 Sprint Management
- Create Sprints
- Start / End Sprint
- Sprint Issue Management

### 📊 Kanban Board
- Drag & Drop Issues
- Status Updates
- Real-time Board Refresh

### ⏱ Work Logs
- Add Work Logs
- Edit Work Logs
- Delete Work Logs
- Time Tracking

### 📎 Attachments
- Upload Attachments
- Download Attachments
- File Validation

### 🔔 Notifications
- Assignment Notifications
- Status Change Notifications
- Sprint Notifications
- Due Date Reminders
- Mark Read / Unread

### 👤 User Profile
- Update Personal Information
- Upload Avatar
- Password Management
- Email Verification Flow
- Audit Logs

---

# 🛠 Tech Stack

## Frontend
- Next.js 16
- React 19
- TypeScript
- Axios
- Tailwind CSS
- Radix UI
- DnD Kit
- Lucide React

## Backend
- Java 17
- Spring Boot
- Spring Web
- Spring Data MongoDB
- Maven
- JavaMailSender

## Database
- MongoDB Atlas

## Deployment
- Vercel
- Render

---

# 📁 Project Structure

```
jira-clone-anjali/
│
├── client/              # Next.js Frontend
│   ├── app/
│   ├── components/
│   ├── lib/
│   └── public/
│
├── server/              # Spring Boot Backend
│   ├── src/
│   ├── uploads/
│   └── pom.xml
│
└── README.md
```

---

# ⚙️ Installation

## Clone Repository

```bash
git clone https://github.com/Anjali503/jira-clone-anjali.git
cd jira-clone-anjali
```

---

## Backend Setup

```bash
cd server
./mvnw spring-boot:run
```

---

## Frontend Setup

```bash
cd client
npm install
npm run dev
```

---

# 🔑 Environment Variables

### Backend

```
MONGODB_URI=
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
APP_BASE_URL=
```

### Frontend

```
NEXT_PUBLIC_API_URL=
```

---

# 📸 Screenshots

> Add screenshots of:

- Login
- Dashboard
- Kanban Board
- Project Page
- Issue Details
- Notifications
- User Profile

---

# 📈 Future Enhancements

- JWT Authentication
- Role-Based Access Control
- Real-Time Collaboration
- Activity Timeline
- Search & Filters
- Analytics Dashboard
- Email Notifications
- Dark Mode

---

# 👨‍💻 Author

**Anjali Agrahari**

GitHub: https://github.com/Anjali503

LinkedIn: *(Add your LinkedIn URL here)*

---

# 📄 License

This project was developed as part of an internship assignment for educational purposes.
