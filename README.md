# SmartMailer

SmartMailer is a full-stack bulk email campaign manager. Users can register, connect their Gmail account with a Gmail App Password, create personalized email campaigns, upload CSV/Excel recipient files, review generated emails, attach PDF/DOCX files, and send emails page by page or by page range.

## Tech Stack

- Frontend: React, TypeScript, Vite, Tailwind CSS, React Query
- Backend: Spring Boot, Spring Security, JWT, Spring Data JPA
- Database: H2 for local development, MySQL for production
- Email: Gmail SMTP using each user's saved Gmail App Password
- Attachments: Local filesystem storage
- Optional queue: RabbitMQ-ready, disabled by default

## Project Structure

```text
SmartMailer/
  backend/    Spring Boot REST API
  frontend/   React TypeScript frontend
```

## Main Features

- User registration and login with JWT authentication
- Gmail App Password setup after login
- Campaign draft creation and resume support
- CSV/Excel recipient upload
- Dynamic template rendering using placeholders like `{{name}}`, `{{company}}`, and `{{role}}`
- Batch review page with 10 recipients per page
- Edit individual generated emails before sending
- Upload, view, download, and delete campaign attachments
- PDF preview and DOCX download support
- Send current page, selected recipients, or a page range
- Wait controls between emails/pages to reduce Gmail throttling risk
- Dashboard with sent, failed, and pending counts

## Local Development

### Prerequisites

- Java 21
- Maven
- Node.js and npm

### Run Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend runs on:

```text
http://localhost:8080
```

### Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on:

```text
http://localhost:5173
```

During development, Vite proxies `/api` requests to `http://localhost:8080`.

## Local Database

By default, the backend uses an in-memory H2 database:

```text
jdbc:h2:mem:smartmailer
```

This means local data can disappear after backend restart. For deployment, use MySQL.

H2 console:

```text
http://localhost:8080/h2-console
```

## Gmail Sending Flow

SmartMailer does not use one global Gmail account for every user.

The intended flow is:

1. User registers with their email.
2. User logs in.
3. User goes to Settings.
4. User adds a Gmail App Password.
5. Campaign emails are sent through that user's Gmail account.

Example:

```text
User: aman@gmail.com
Saved Gmail App Password: ****-****-****-****
Campaign emails are sent from: aman@gmail.com
```

Normal Gmail passwords do not work. Google requires a Gmail App Password.

## How To Generate Gmail App Password

1. Open Google Account.
2. Go to Security.
3. Enable 2-Step Verification.
4. Open App passwords.
5. Create an app password for `SmartMailer`.
6. Copy the 16-character password.
7. Paste it in SmartMailer Settings.

Google App Password page:

```text
https://myaccount.google.com/apppasswords
```

## Environment Variables

The backend reads configuration from environment variables.

### Development Defaults

These are already configured in `backend/src/main/resources/application.yml`:

```env
DB_URL=jdbc:h2:mem:smartmailer;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
DB_USERNAME=sa
DB_PASSWORD=
DB_DRIVER=org.h2.Driver
CORS_ALLOWED_ORIGINS=http://localhost:5173
UPLOAD_DIR=uploads
EMAIL_QUEUE_ENABLED=false
```

### Production Example

Use MySQL and a strong JWT secret:

```env
DB_URL=jdbc:mysql://localhost:3306/smartmailer
DB_USERNAME=smartmailer_user
DB_PASSWORD=change_this_password
DB_DRIVER=com.mysql.cj.jdbc.Driver

JWT_SECRET=change-this-to-a-long-random-production-secret
JWT_EXPIRES_MINUTES=1440

CORS_ALLOWED_ORIGINS=https://yourdomain.com
UPLOAD_DIR=/var/smartmailer/uploads

EMAIL_QUEUE_ENABLED=false
MAIL_FROM=no-reply@yourdomain.com
```

## Build Commands

### Backend

```bash
cd backend
mvn clean package
```

Generated jar:

```text
backend/target/smartmailer-backend-0.0.1-SNAPSHOT.jar
```

### Frontend

```bash
cd frontend
npm install
npm run build
```

Generated frontend files:

```text
frontend/dist
```

## Production Deployment Overview

Recommended setup:

```text
Nginx -> React static files
Nginx /api -> Spring Boot backend on port 8080
Spring Boot -> MySQL
Spring Boot -> Gmail SMTP per user
```

### 1. Install Server Packages

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk mysql-server nginx
```

### 2. Create MySQL Database

```sql
CREATE DATABASE smartmailer;
CREATE USER 'smartmailer_user'@'localhost' IDENTIFIED BY 'strong_password_here';
GRANT ALL PRIVILEGES ON smartmailer.* TO 'smartmailer_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure Environment File

Create:

```bash
sudo nano /etc/smartmailer.env
```

Add production values:

```env
DB_URL=jdbc:mysql://localhost:3306/smartmailer
DB_USERNAME=smartmailer_user
DB_PASSWORD=strong_password_here
DB_DRIVER=com.mysql.cj.jdbc.Driver
JWT_SECRET=replace-with-long-random-secret
CORS_ALLOWED_ORIGINS=https://yourdomain.com
UPLOAD_DIR=/var/smartmailer/uploads
EMAIL_QUEUE_ENABLED=false
```

Create upload directory:

```bash
sudo mkdir -p /var/smartmailer/uploads
sudo chown -R ubuntu:ubuntu /var/smartmailer
```

Replace `ubuntu` with your server username if different.

### 4. Create Backend Service

Create:

```bash
sudo nano /etc/systemd/system/smartmailer.service
```

Example:

```ini
[Unit]
Description=SmartMailer Backend
After=network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/SmartMailer/backend
EnvironmentFile=/etc/smartmailer.env
ExecStart=/usr/bin/java -jar /home/ubuntu/SmartMailer/backend/target/smartmailer-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Start service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable smartmailer
sudo systemctl start smartmailer
sudo systemctl status smartmailer
```

View logs:

```bash
journalctl -u smartmailer -f
```

### 5. Deploy Frontend

```bash
sudo mkdir -p /var/www/smartmailer
sudo cp -r frontend/dist/* /var/www/smartmailer/
```

### 6. Configure Nginx

Create:

```bash
sudo nano /etc/nginx/sites-available/smartmailer
```

Example:

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    root /var/www/smartmailer;
    index index.html;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Authorization $http_authorization;
    }

    location / {
        try_files $uri /index.html;
    }
}
```

Enable site:

```bash
sudo ln -s /etc/nginx/sites-available/smartmailer /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 7. Add HTTPS

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com
```

## Gmail Limits

Gmail may block or throttle accounts that send too many emails too quickly. SmartMailer includes wait controls between emails and pages, but Gmail limits still apply.

For higher-volume production sending, use a provider such as Amazon SES, SendGrid, or Mailgun instead of personal Gmail SMTP.

## Useful Commands

Run backend tests:

```bash
cd backend
mvn test
```

Build frontend:

```bash
cd frontend
npm run build
```

Restart backend service:

```bash
sudo systemctl restart smartmailer
```

Check backend logs:

```bash
journalctl -u smartmailer -f
```

## Notes

- Do not deploy with H2 in-memory database.
- Do not put user Gmail passwords in `application.yml`.
- Users add their own Gmail App Password in Settings after login.
- Attachments are stored in `UPLOAD_DIR`; make sure this directory is persistent.
- Keep `JWT_SECRET` private and strong in production.
