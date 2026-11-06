
# Podcasts API

REST API для управления публикацией подкастов.

Проект разработан в рамках лабораторных работ №3–4 по дисциплине «Разработка безопасного программного обеспечения».

**Вариант 12 — «Публикация подкастов».**

## 1. Описание проекта

Приложение позволяет управлять пользователями, подкастами, эпизодами, аудиофайлами и расписанием публикаций.

Реализованы создание, просмотр, редактирование и удаление данных через REST API.

Для хранения информации используется PostgreSQL.

Взаимодействие с базой данных осуществляется через Spring Data JPA и Hibernate.

## 2. Используемые технологии

- Java 21
- Spring Boot 3.5.7
- Spring Web
- Spring Data JPA
- Hibernate
- PostgreSQL
- Maven
- IntelliJ IDEA
- pgAdmin 4
- Spring Security (HTTP Basic, CSRF, method security)
- BCrypt

## Безопасность (лабораторная №4)

- Регистрация: `POST /api/auth/register` или сохранённый адрес `POST /api/users`.
- Перед любым `POST`, `PATCH` или `DELETE` нужно выполнить `GET /api/csrf`, сохранить cookie `XSRF-TOKEN` и передать полученный токен в заголовке `X-XSRF-TOKEN`.
- Остальные запросы используют HTTP Basic; логином является email.
- Пароль: 12–72 символа, строчная и заглавная буквы, цифра и специальный символ. В БД хранится только BCrypt-хеш.
- Публичная регистрация всегда создаёт `USER` в состоянии `ACTIVE`; роль и состояние может менять только `ADMIN`.
- `USER` работает только со своей учётной записью и со своей цепочкой данных. Списки всех сущностей доступны только `ADMIN`.
- Заблокированная учётная запись не проходит аутентификацию.

Первый администратор создаётся при запуске только из переменных окружения (секрет в проект не записывается):

```text
APP_ADMIN_EMAIL=admin@example.com
APP_ADMIN_PASSWORD=ChangeThisAdmin1!
APP_ADMIN_NAME=Administrator
```

После создания переменную пароля можно убрать. Полный набор положительных и отрицательных сценариев находится в `requests.http`.

Старые записи лабораторной №3 без пароля при первом запуске автоматически блокируются и получают непредсказуемый BCrypt-хеш. Их можно безопасно восстановить через настройки администратора; открытые или стандартные пароли им не назначаются.

## 3. Структура проекта

Основные пакеты приложения:

```text
src/main/java/com/example/podcasts/
│
├── controller/
│   ├── UserController.java
│   ├── PodcastController.java
│   ├── EpisodeController.java
│   ├── AudioFileController.java
│   └── PublicationSlotController.java
│
├── service/
│   ├── UserService.java
│   ├── PodcastService.java
│   ├── EpisodeService.java
│   ├── AudioFileService.java
│   └── PublicationSlotService.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PodcastRepository.java
│   ├── EpisodeRepository.java
│   ├── AudioFileRepository.java
│   └── PublicationSlotRepository.java
│
├── model/
│   ├── User.java
│   ├── Podcast.java
│   ├── Episode.java
│   ├── AudioFile.java
│   └── PublicationSlot.java
│
└── PodcastsApplication.java
```

Назначение пакетов:

- `controller` — обработка HTTP-запросов.
- `service` — бизнес-логика приложения.
- `repository` — взаимодействие с PostgreSQL.
- `model` — сущности приложения.

## 4. База данных

В проекте используется PostgreSQL.

Название базы данных:

```text
podcasts_db
```

В базе данных создаются пять таблиц:

| Таблица | Назначение |
|---|---|
| users | Пользователи |
| podcasts | Подкасты |
| episodes | Эпизоды подкастов |
| audio_files | Аудиофайлы эпизодов |
| publication_slots | Расписание публикаций |

### Связи между таблицами

- Один пользователь может создавать несколько подкастов.
- Один подкаст может содержать несколько эпизодов.
- Один эпизод может иметь несколько аудиофайлов.
- Слот публикации связан с подкастом и эпизодом.

Для обеспечения целостности данных используются внешние ключи.

## 5. Настройка PostgreSQL

Перед запуском приложения необходимо установить PostgreSQL.

Создать базу данных:

```sql
CREATE DATABASE podcasts_db;
```

Для подключения приложения можно использовать отдельного пользователя PostgreSQL.

В проекте используется пользователь:

```text
podcasts_app
```

Пользователю необходимо предоставить права на подключение к базе и работу с её таблицами.

### Настройка application.properties

Файл:

```text
src/main/resources/application.properties
```

Содержимое:

```properties
spring.application.name=podcasts

server.port=8080
server.error.include-message=always

spring.datasource.url=jdbc:postgresql://localhost:5432/podcasts_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Имя пользователя и пароль базы данных передаются через переменные окружения:

```text
DB_USERNAME
DB_PASSWORD
```

Пароль не должен храниться непосредственно в исходном коде или загружаться на GitHub.

Параметр:

```properties
spring.jpa.hibernate.ddl-auto=update
```

позволяет Hibernate обновлять структуру таблиц на основании JPA-сущностей.

## 6. Запуск приложения

1. Открыть проект в IntelliJ IDEA.
2. Убедиться, что установлен JDK 21 или совместимая версия Java.
3. Запустить PostgreSQL.
4. Создать базу данных `podcasts_db`.
5. Настроить переменные окружения `DB_USERNAME` и `DB_PASSWORD`.
6. Дождаться загрузки Maven-зависимостей.
7. Запустить класс `PodcastsApplication`.

Также приложение можно запустить через Maven Wrapper:

Для Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Переменные окружения должны быть доступны процессу запуска.

После успешного запуска приложение работает на порту `8080`.

Базовый адрес API:

```text
http://localhost:8080/api
```

## 7. REST API

Приложение предоставляет следующие группы HTTP-запросов.

### Пользователи

| Метод | Адрес | Описание |
|---|---|---|
| GET | /api/users | Получить пользователей |
| GET | /api/users/{id} | Получить пользователя |
| POST | /api/users | Создать пользователя |
| PATCH | /api/users/{id} | Изменить пользователя |
| DELETE | /api/users/{id} | Удалить пользователя |

### Подкасты

| Метод | Адрес | Описание |
|---|---|---|
| GET | /api/podcasts | Получить подкасты |
| GET | /api/podcasts/{id} | Получить подкаст |
| POST | /api/podcasts | Создать подкаст |
| PATCH | /api/podcasts/{id} | Изменить подкаст |
| DELETE | /api/podcasts/{id} | Удалить подкаст |

### Эпизоды

| Метод | Адрес | Описание |
|---|---|---|
| GET | /api/episodes | Получить эпизоды |
| GET | /api/episodes/{id} | Получить эпизод |
| POST | /api/episodes | Создать эпизод |
| PATCH | /api/episodes/{id} | Изменить эпизод |
| DELETE | /api/episodes/{id} | Удалить эпизод |

### Аудиофайлы

| Метод | Адрес | Описание |
|---|---|---|
| GET | /api/audio-files | Получить аудиофайлы |
| GET | /api/audio-files/{id} | Получить аудиофайл |
| POST | /api/audio-files | Добавить аудиофайл |
| PATCH | /api/audio-files/{id} | Изменить аудиофайл |
| DELETE | /api/audio-files/{id} | Удалить аудиофайл |

### Расписание публикаций

| Метод | Адрес | Описание |
|---|---|---|
| GET | /api/publication-slots | Получить слоты публикации |
| GET | /api/publication-slots/{id} | Получить слот |
| POST | /api/publication-slots | Создать слот |
| PATCH | /api/publication-slots/{id} | Изменить слот |
| DELETE | /api/publication-slots/{id} | Удалить слот |

## 8. Примеры HTTP-запросов

### Создание пользователя

```http
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "name": "Тестовый пользователь",
  "email": "test@example.com"
}
```

При успешном создании сервер возвращает статус `201 Created`.

### Создание подкаста

```http
POST http://localhost:8080/api/podcasts
Content-Type: application/json

{
  "title": "Разговоры о Java",
  "authorId": 1,
  "status": "ACTIVE"
}
```

### Создание эпизода

```http
POST http://localhost:8080/api/episodes
Content-Type: application/json

{
  "podcastId": 1,
  "title": "Выпуск 1. Spring Boot",
  "durationSeconds": 1800
}
```

### Добавление аудиофайла

```http
POST http://localhost:8080/api/audio-files
Content-Type: application/json

{
  "episodeId": 1,
  "path": "/audio/ep1.mp3",
  "format": "MP3",
  "sizeBytes": 5000000
}
```

### Создание слота публикации

```http
POST http://localhost:8080/api/publication-slots
Content-Type: application/json

{
  "podcastId": 1,
  "episodeId": 1,
  "dateTime": "2030-01-15T12:00:00"
}
```

Идентификаторы в примерах необходимо заменять на значения, полученные при создании соответствующих записей.

Дополнительные примеры запросов находятся в файле `requests.http`.

## 9. Бизнес-логика приложения

### 9.1. Основной аудиофайл

У эпизода может быть несколько аудиофайлов.

При добавлении первого аудиофайла он автоматически становится основным.

Если другой аудиофайл назначается основным, у предыдущего основного файла значение `main` изменяется на `false`.

Таким образом, у эпизода не должно быть одновременно нескольких основных аудиофайлов.

### 9.2. Планирование публикации

Для создания слота публикации необходимо:

1. Создать подкаст.
2. Создать эпизод.
3. Добавить основной аудиофайл.
4. Указать дату и время публикации.

Если основной аудиофайл отсутствует, приложение отклоняет создание слота.

Дата публикации должна находиться в будущем.

После успешного планирования состояние эпизода изменяется:

```text
DRAFT → SCHEDULED
```

### 9.3. Завершение публикации

После изменения статуса слота на `DONE` эпизод считается опубликованным.

Состояние эпизода изменяется:

```text
SCHEDULED → PUBLISHED
```

### 9.4. Каскадное удаление

При удалении пользователя приложение удаляет связанные с ним данные:

```text
Пользователь
    ↓
Подкасты
    ↓
Эпизоды
    ↓
Аудиофайлы и слоты публикации
```

Это предотвращает появление записей, ссылающихся на удалённые объекты.

## 10. Проверка работы

Для тестирования использовался HTTP-клиент IntelliJ IDEA.

Проверены следующие сценарии:

1. Создание пользователя.
2. Создание подкаста.
3. Создание эпизода.
4. Добавление основного аудиофайла.
5. Замена основного аудиофайла.
6. Создание слота публикации.
7. Изменение состояния эпизода на `SCHEDULED`.
8. Завершение публикации и изменение состояния на `PUBLISHED`.
9. Удаление аудиофайла.
10. Удаление пользователя со связанными данными.
11. Получение ошибки `404` при обращении к удалённому подкасту.
12. Проверка сохранения данных после перезапуска приложения.

### Проверка каскадного удаления в PostgreSQL

Для проверки количества записей использовался SQL-запрос:

```sql
SELECT 'users' AS table_name, COUNT(*) AS records FROM users
UNION ALL
SELECT 'podcasts', COUNT(*) FROM podcasts
UNION ALL
SELECT 'episodes', COUNT(*) FROM episodes
UNION ALL
SELECT 'audio_files', COUNT(*) FROM audio_files
UNION ALL
SELECT 'publication_slots', COUNT(*) FROM publication_slots;
```

После удаления тестового пользователя все пять таблиц содержали ноль записей.

### Проверка постоянного хранения

После создания нового пользователя приложение было остановлено и запущено повторно.

Повторный запрос:

```http
GET http://localhost:8080/api/users
```

подтвердил, что пользователь сохранился в PostgreSQL.

## 11. Результат работы

В ходе лабораторной работы разработанное REST API было переведено с хранения данных в оперативной памяти на PostgreSQL.

Для взаимодействия с базой данных использованы Spring Data JPA и Hibernate.

Реализованы связи между сущностями и сохранена бизнес-логика управления публикацией подкастов.

Проверены основные HTTP-операции, каскадное удаление и сохранение данных после перезапуска приложения.
