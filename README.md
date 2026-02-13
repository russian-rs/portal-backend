# Backend сервис Портала Волонтера

### Зависимости приложения

Для локального запуска достаточно (см `docker-compose.yml`):

1. PostgreSQL - для хранения данных
2. Redis - для хранения пользовательских сессий
3. S3 - для хранения

На production среде также есть:

4. Authentik - для авторизации пользователей по технологии Single Sign-on
5. Wordpress - туда синхронизируются пользователи (посколько основной сайт на WP)
6. Cloudflare Turnstile - для проверки Captchа токенов
7. SMTP сервер - для отправки e-mail

### Локальный запуск

`docker-compose up -d` (без `-d` чтобы смотреть сразу логи)  
Запускать main в файле `PortalBackendApplication.kt`  
Указывать spring-профили `local,no-auth`

### Быстрый старт

api лежит в `api/openapi.yaml` и генерится автоматически при билде или при запуске gradle таски `api/openapi.yaml`  
Этот файл можно перетащить в postman и он сгенерит удобно api для тестирования, останется только прописать baseUrl =
`http://localhost:8081/` в переменные постмана

### Тесты

- Для запуска контейнеров docker-compose-test

### Безопасность

Все проверки безопасности запускаются автоматически в GitHub Actions при push/PR.

#### 1. Pre-commit проверки

**Автоматическая установка**:

```bash
git config core.hooksPath .git-hooks
```

**Ручной запуск**:

```bash
./scripts/security-check.sh
```

**Полное сканирование** (требует установки `gitleaks`, `exiftool`):

```bash
export SKIP_OPTIONAL_TOOLS=false
./scripts/security-check.sh
```

**Что проверяется**: секреты, большие файлы, сертификаты, backup файлы, gitleaks

#### 2. OWASP Dependency Check

**Локальный запуск**:

```bash
./gradlew dependencyCheckAnalyze
```

Отчет: `service/build/reports/dependency-check-report.html`

**NVD API ключ** (опционально, ускоряет проверку):

1. Получить: https://nvd.nist.gov/developers/request-an-api-key
2. Локально: `export NVD_API_KEY=your-key`
3. GitHub: Settings → Secrets → Repository secrets → `NVD_API_KEY`
