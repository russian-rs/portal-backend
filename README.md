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
Этот файл можно перетащить в postman и он сгенерит удобно api для тестирования, останется только прописать baseUrl = `http://localhost:8081/` в переменные постмана

### Тесты 
- Для запуска использовать docker-compose-test.
- Указывать профили "local", "no-auth", "test"

## Security Checks

Проверки безопасности запускаются автоматически в GitHub Actions при создании PR.

**Локальная установка**:
```bash
git config core.hooksPath .git-hooks
```

**Ручной запуск**: `./scripts/security-check.sh`

**Полное сканирование**: 
Необходимо установить инструменты `gitleaks`, `exiftool`, затем выполнить:
```bash
export SKIP_OPTIONAL_TOOLS=false

./scripts/security-check.sh`
```
