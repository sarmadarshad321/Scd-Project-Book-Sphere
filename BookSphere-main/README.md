# BookSphere

A comprehensive web-based library management platform built with Spring Boot, featuring role-based authentication (Admin and Student), book management, transaction tracking, and fine calculation.

## 🎯 Project Overview

BookSphere is designed for educational institutions to efficiently manage their library resources, user accounts, and book transactions. The system implements core Software Design and Construction (SCD) concepts including design patterns, SOLID principles, and clean architecture.

## ✨ Key Features

### Admin Features
- 📚 **Complete Book Management**: Add, edit, delete, and manage books
- 👥 **User Management**: Create and manage student accounts
- 📤 **Issue/Return Books**: Process book transactions
- 💰 **Fine Management**: Calculate and collect overdue fines
- 📊 **Reports & Analytics**: Comprehensive reporting system
- 🏷️ **Category Management**: Organize books by categories
- ⚙️ **System Configuration**: Manage system settings

### Student Features
- 🔍 **Search & Browse**: Find books easily with advanced search
- 📖 **Book Reservation**: Reserve books for borrowing
- 📚 **My Books**: View currently borrowed books
- 📜 **History**: Track borrowing history
- 💳 **Fine Tracking**: View and manage pending fines
- 👤 **Profile Management**: Update personal information
- 🔔 **Notifications**: Receive due date reminders

## 🏗️ Architecture & Design Patterns

This project implements several design patterns:
- **Strategy Pattern**: Fine calculation strategies
- **Repository Pattern**: Data access abstraction
- **MVC Pattern**: Separation of concerns
- **Singleton Pattern**: Service layer beans
- **Factory Pattern**: Recommendation engine factory

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: PostgreSQL 15
- **Security**: Spring Security with BCrypt
- **Frontend**: Thymeleaf, Bootstrap 5.3.2, Bootstrap Icons
- **ORM**: Hibernate/JPA
- **Build Tool**: Maven 3.9+
- **Testing**: JUnit 5, Mockito
- **Containerization**: Docker

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- PostgreSQL 15 (or Docker)
- Docker (optional, for database)

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd project
```

### 2. Configure Environment Variables

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` file with your database credentials:

```properties
DB_HOST=localhost
DB_PORT=5433
DB_NAME=library_db
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

### 3. Start PostgreSQL Database

**Option A: Using Docker (Recommended)**

```bash
docker run -d \
  --name mnrpg \
  -e POSTGRES_USER=your_username \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=library_db \
  -p 5433:5432 \
  postgres:15-alpine
```

**Option B: Local PostgreSQL**

Create a database named `library_db` and update the `.env` file accordingly.

### 4. Load Environment Variables

```bash
export $(cat .env | xargs)
```

### 5. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

## 👥 Default Credentials

**Admin Account:**
- Username: `admin`
- Password: `admin123`

**Student Account:**
- Username: `student1`
- Password: `student123`

⚠️ **Important**: Change these credentials after first login!

## 📁 Project Structure

```
library-management-system/
│
├── app/
│   ├── __init__.py                 # Application factory
│   │
│   ├── models/                     # Database models
│   │   ├── __init__.py
│   │   ├── user.py                # User model
│   │   ├── book.py                # Book model
│   │   ├── transaction.py         # Transaction model
│   │   ├── category.py            # Category model
│   │   └── fine.py                # Fine model
│   │
│   ├── controllers/                # Route controllers
│   │   ├── __init__.py
│   │   ├── auth_controller.py     # Authentication routes
│   │   ├── admin_controller.py    # Admin routes
│   │   ├── student_controller.py  # Student routes
│   │   └── book_controller.py     # Book routes
│   │
│   ├── services/                   # Business logic
│   │   ├── __init__.py
│   │   ├── auth_service.py        # Authentication service
│   │   ├── book_service.py        # Book service
│   │   ├── transaction_service.py # Transaction service
│   │   └── notification_service.py# Notification service
│   │
│   ├── utils/                      # Utility functions
│   │   ├── __init__.py
│   │   ├── decorators.py          # Custom decorators
│   │   ├── validators.py          # Form validators
│   │   └── helpers.py             # Helper functions
│   │
│   ├── templates/                  # HTML templates
│   │   ├── base.html              # Base template
│   │   ├── auth/                  # Authentication templates
│   │   │   ├── login.html
│   │   │   └── register.html
│   │   ├── admin/                 # Admin templates
│   │   │   ├── dashboard.html
│   │   │   ├── books.html
│   │   │   └── users.html
│   │   └── student/               # Student templates
│   │       ├── dashboard.html
│   │       ├── browse.html
│   │       └── my_books.html
│   │
│   └── static/                     # Static files
│       ├── css/
│       │   └── style.css
│       ├── js/
│       │   └── main.js
│       └── images/
│           └── logo.png
│
├── migrations/                     # Database migrations
├── tests/                          # Test files
│   ├── test_models.py
│   ├── test_auth.py
│   └── test_transactions.py
│
├── config.py                       # Configuration settings
├── requirements.txt                # Python dependencies
├── run.py                          # Application entry point
├── seed.py                         # Database seeder
├── .env.example                    # Example environment file
├── .gitignore                      # Git ignore file
├── DEVELOPMENT_PLAN.md            # Development roadmap
├── PROJECT_OVERVIEW.md            # Detailed project information
└── README.md                      # This file
```

## 🗄️ Database Schema

### Users Table
- id, username, email, password_hash, full_name, phone, address, role, is_active, created_at, updated_at

### Books Table
- id, title, author, isbn, publisher, publication_year, category_id, quantity, available_quantity, description, cover_image, created_at, updated_at

### Categories Table
- id, name, description, created_at, updated_at

### Transactions Table
- id, book_id, user_id, issue_date, due_date, return_date, status, fine_amount, created_at, updated_at

### Reservations Table
- id, book_id, user_id, reservation_date, status, fulfilled_date, created_at, updated_at

### Fines Table
- id, transaction_id, user_id, amount, paid_amount, status, payment_date, created_at, updated_at

## 🧪 Testing

Run tests using pytest:
```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=app tests/

# Run specific test file
pytest tests/test_auth.py
```

## 📊 Development Phases

The project is developed in 9 phases:
1. ✅ Project Setup & Database Design
2. ⏳ Authentication & User Management
3. ⏳ Admin Module Development
4. ⏳ Student Module Development
5. ⏳ Advanced Features & Business Logic
6. ⏳ UI/UX Implementation
7. ⏳ SCD Concepts Implementation
8. ⏳ Testing & Quality Assurance
9. ⏳ Documentation & Deployment

See [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) for detailed phase information.

## 🔒 Security Features

- Password hashing using Werkzeug security
- Session-based authentication
- CSRF protection
- SQL injection prevention (SQLAlchemy ORM)
- XSS protection (template escaping)
- Role-based access control
- Input validation and sanitization

## 📝 API Endpoints

### Authentication
- `POST /auth/login` - User login
- `POST /auth/logout` - User logout
- `POST /auth/register` - User registration (Admin only)

### Admin Routes
- `GET /admin/dashboard` - Admin dashboard
- `GET /admin/books` - List all books
- `POST /admin/books/add` - Add new book
- `PUT /admin/books/<id>` - Update book
- `DELETE /admin/books/<id>` - Delete book
- `GET /admin/users` - List all users
- `POST /admin/issue` - Issue book
- `POST /admin/return` - Return book

### Student Routes
- `GET /student/dashboard` - Student dashboard
- `GET /student/books` - Browse books
- `GET /student/books/<id>` - Book details
- `POST /student/reserve` - Reserve book
- `GET /student/my-books` - Current loans
- `GET /student/history` - Borrowing history
- `GET /student/fines` - View fines

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is developed as an academic project for the Software Design and Construction course.

## 👨‍💻 Author

**Your Name**
- Course: Software Design and Construction
- Institution: [Your Institution]
- Session: [Academic Session]

## 🙏 Acknowledgments

- Flask documentation and community
- Bootstrap for the frontend framework
- SQLAlchemy for the ORM
- Course instructors and mentors

## 📞 Support

For support, email your-email@example.com or open an issue in the repository.

## 🔄 Version History

- **v1.0.0** - Initial release with core features
  - Authentication system
  - Book management
  - Transaction processing
  - Fine calculation

---

**Status**: Development Phase - Phase 1 Ready to Start  
**Last Updated**: January 12, 2026  
**Version**: 1.0.0
