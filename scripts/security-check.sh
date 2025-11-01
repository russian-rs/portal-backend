#!/usr/bin/env bash
#
# Security Check Script
# Used by both git hooks and CI/CD
#
# Environment variables:
#   CHECK_MODE=hook|ci  - Determines which files to check
#   SKIP_OPTIONAL_TOOLS=true - Skip checks that require optional tools

set -e

# Configuration
MAX_FILE_SIZE_MB=${MAX_FILE_SIZE_MB:-1}
CHECK_MODE=${CHECK_MODE:-hook}
SKIP_OPTIONAL_TOOLS=${SKIP_OPTIONAL_TOOLS:-false}

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track if any issues found
ERRORS_FOUND=0
WARNINGS_FOUND=0

# Helper functions
print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
    ERRORS_FOUND=$((ERRORS_FOUND + 1))
}

print_warning() {
    echo -e "${YELLOW}⚠ WARNING: $1${NC}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Get list of files to check based on mode
get_files_to_check() {
    if [ "$CHECK_MODE" = "ci" ]; then
        # CI mode: check changed files from environment variable
        if [ -n "$CHANGED_FILES" ]; then
            echo "$CHANGED_FILES"
        else
            echo ""
        fi
    else
        # Hook mode: check staged files
        git diff --cached --name-only --diff-filter=ACM
    fi
}

FILES_TO_CHECK=$(get_files_to_check)

if [ -z "$FILES_TO_CHECK" ]; then
    print_success "No files to check"
    exit 0
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Security Scan ($CHECK_MODE mode)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# =============================================================================
# CHECK 1: Suspicious Filenames
# =============================================================================
print_info "Checking for suspicious filenames..."

BLOCKED_PATTERNS=(
    "\.env$"
    "\.env\.local$"
    "\.env\.prod"
    "\.env\.production"
    "credentials\.json$"
    "secrets\.ya?ml$"
    "private.*\.key$"
    "\.aws/credentials$"
    "\.ssh/id_rsa$"
    "\.kube/config$"
    "password.*\.txt$"
)

for pattern in "${BLOCKED_PATTERNS[@]}"; do
    matches=$(echo "$FILES_TO_CHECK" | grep -E "$pattern" || true)
    if [ -n "$matches" ]; then
        print_error "Suspicious filename pattern '$pattern' found:"
        echo "$matches" | sed 's/^/  /'
    fi
done

# =============================================================================
# CHECK 2: Binary Files
# =============================================================================
print_info "Checking for unexpected binary files..."

echo "$FILES_TO_CHECK" | while IFS= read -r file; do
    [ -z "$file" ] || [ ! -f "$file" ] && continue

    file_type=$(file -b "$file")
    if ! echo "$file_type" | grep -q "text\|empty\|JSON\|XML"; then
        # Allow common development binaries
        if ! echo "$file" | grep -qE "\.(jar|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot)$"; then
            print_warning "Unexpected binary file: $file ($file_type)"
        fi
    fi
done

# =============================================================================
# CHECK 3: Large Files
# =============================================================================
print_info "Checking for large files (>${MAX_FILE_SIZE_MB}MB)..."

echo "$FILES_TO_CHECK" | while IFS= read -r file; do
    [ -z "$file" ] || [ ! -f "$file" ] && continue

    file_size_mb=$(du -m "$file" | cut -f1)
    if [ "$file_size_mb" -gt "$MAX_FILE_SIZE_MB" ]; then
        print_error "Large file detected: $file (${file_size_mb}MB)"
        echo "  Consider using Git LFS or excluding this file"
    fi
done

# =============================================================================
# CHECK 4: Image EXIF Metadata (Optional Tool)
# =============================================================================
if [ "$SKIP_OPTIONAL_TOOLS" = "false" ] && command -v exiftool &> /dev/null; then
    print_info "Checking for sensitive EXIF metadata in images..."

    IMAGE_FILES=$(echo "$FILES_TO_CHECK" | grep -E "\.(jpg|jpeg|png|gif)$" || true)
    if [ -n "$IMAGE_FILES" ]; then
        echo "$IMAGE_FILES" | while IFS= read -r img; do
            [ -z "$img" ] || [ ! -f "$img" ] && continue

            exif_data=$(exiftool "$img" 2>/dev/null | grep -iE "author|creator|owner|gps|location|copyright|artist" || true)
            if [ -n "$exif_data" ]; then
                print_warning "EXIF metadata found in: $img"
                echo "$exif_data" | sed 's/^/  /'
                echo "  → Strip metadata: exiftool -all= $img"
            fi
        done
    fi
elif [ "$SKIP_OPTIONAL_TOOLS" = "false" ]; then
    print_info "Skipping EXIF check (exiftool not installed)"
fi

# =============================================================================
# CHECK 5: Backup Files
# =============================================================================
print_info "Checking for backup files..."

BACKUP_PATTERNS="\.(bak|backup|old|orig|swp|swo|tmp)$|~$"
backup_files=$(echo "$FILES_TO_CHECK" | grep -E "$BACKUP_PATTERNS" || true)
if [ -n "$backup_files" ]; then
    print_error "Backup files should not be committed:"
    echo "$backup_files" | sed 's/^/  /'
    echo "  → Add these extensions to .gitignore"
fi

# =============================================================================
# CHECK 6: Certificates & Private Keys
# =============================================================================
print_info "Checking for certificates and private keys..."

CERT_PATTERNS="\.(pem|crt|cer|key|p12|pfx|jks|keystore)$"
cert_files=$(echo "$FILES_TO_CHECK" | grep -E "$CERT_PATTERNS" || true)
if [ -n "$cert_files" ]; then
    print_error "Certificate/key files detected:"
    echo "$cert_files" | sed 's/^/  /'
    echo "  → Never commit private keys or production certificates!"
fi

# =============================================================================
# CHECK 7: Database Dumps
# =============================================================================
print_info "Checking for database dumps..."

DUMP_PATTERNS="\.(sql|dump|backup\.sql)$"
dump_files=$(echo "$FILES_TO_CHECK" | grep -E "$DUMP_PATTERNS" || true)
if [ -n "$dump_files" ]; then
    print_warning "Database dump files detected:"
    echo "$dump_files" | sed 's/^/  /'
    echo "  → Ensure these don't contain real data"
fi

# =============================================================================
# CHECK 8: Hardcoded URLs
# =============================================================================
print_info "Checking for hardcoded URLs..."

echo "$FILES_TO_CHECK" | while IFS= read -r file; do
    [ -z "$file" ] || [ ! -f "$file" ] && continue

    if echo "$file" | grep -qE "\.(kt|yaml|yml|md|xml|properties|java|js|ts)$"; then
        if [ "$CHECK_MODE" = "ci" ]; then
            # CI mode: check file contents directly
            urls=$(grep -oE "https?://[^\s'\"\`<>]+" "$file" 2>/dev/null | \
                   grep -vE "localhost|127\.0\.0\.1|0\.0\.0\.0|example\.(com|org|net)|github\.com|gitlab\.com|stackoverflow\.com|maven\.org|gradle\.org|spring\.io" || true)
        else
            # Hook mode: check git diff
            urls=$(git diff --cached "$file" | grep -E "^\+" | grep -oE "https?://[^\s'\"\`<>]+" | \
                   grep -vE "localhost|127\.0\.0\.1|0\.0\.0\.0|example\.(com|org|net)|github\.com|gitlab\.com|stackoverflow\.com|maven\.org|gradle\.org|spring\.io" || true)
        fi

        if [ -n "$urls" ]; then
            print_warning "Hardcoded URL(s) in: $file"
            echo "$urls" | sort -u | sed 's/^/  /'
            echo "  → Consider using environment variables"
        fi
    fi
done

# =============================================================================
# CHECK 9: Hardcoded IP Addresses
# =============================================================================
print_info "Checking for hardcoded IP addresses..."

echo "$FILES_TO_CHECK" | while IFS= read -r file; do
    [ -z "$file" ] || [ ! -f "$file" ] && continue

    if echo "$file" | grep -qE "\.(kt|yaml|yml|properties|java|xml)$"; then
        if [ "$CHECK_MODE" = "ci" ]; then
            ips=$(grep -oE "\b([0-9]{1,3}\.){3}[0-9]{1,3}\b" "$file" 2>/dev/null | \
                  grep -vE "^127\.|^0\.0\.0\.0$|^255\.255\.255\.255$|^192\.168\.|^10\.|^172\.(1[6-9]|2[0-9]|3[0-1])\." || true)
        else
            ips=$(git diff --cached "$file" | grep -E "^\+" | grep -oE "\b([0-9]{1,3}\.){3}[0-9]{1,3}\b" | \
                  grep -vE "^127\.|^0\.0\.0\.0$|^255\.255\.255\.255$|^192\.168\.|^10\.|^172\.(1[6-9]|2[0-9]|3[0-1])\." || true)
        fi

        if [ -n "$ips" ]; then
            print_warning "Hardcoded IP address(es) in: $file"
            echo "$ips" | sort -u | sed 's/^/  /'
            echo "  → Use configuration or environment variables"
        fi
    fi
done

# =============================================================================
# CHECK 10: Private Repository URLs
# =============================================================================
print_info "Checking for private repository URLs..."

echo "$FILES_TO_CHECK" | while IFS= read -r file; do
    [ -z "$file" ] || [ ! -f "$file" ] && continue

    if echo "$file" | grep -qE "\.(gradle|kts|xml|yml|yaml)$"; then
        if [ "$CHECK_MODE" = "ci" ]; then
            private_repos=$(grep -iE "nexus|artifactory|jfrog|private.*registry|internal.*repo" "$file" 2>/dev/null || true)
        else
            private_repos=$(git diff --cached "$file" | grep -E "^\+" | grep -iE "nexus|artifactory|jfrog|private.*registry|internal.*repo" || true)
        fi

        if [ -n "$private_repos" ]; then
            print_error "Private repository reference in: $file"
            echo "$private_repos" | sed 's/^/  /'
            echo "  → Remove or replace with public repositories"
        fi
    fi
done

# =============================================================================
# CHECK 11: Secrets Patterns
# =============================================================================
print_info "Checking for hardcoded secrets..."

SECRET_PATTERNS=(
    "password\s*[:=]\s*['\"]?[^'\"\\s]{8,}"
    "(api[_-]?key|apikey)\s*[:=]\s*['\"]?[^'\"\\s]{16,}"
    "(secret[_-]?key|secret)\s*[:=]\s*['\"]?[^'\"\\s]{16,}"
    "(access[_-]?key|access[_-]?token)\s*[:=]\s*['\"]?[^'\"\\s]{16,}"
    "(client[_-]?secret)\s*[:=]\s*['\"]?[^'\"\\s]{16,}"
    "Bearer\s+[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.?[A-Za-z0-9\-_.+/=]*"
    "-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----"
)

echo "$FILES_TO_CHECK" | while IFS= read -r file; do
    [ -z "$file" ] || [ ! -f "$file" ] && continue

    for pattern in "${SECRET_PATTERNS[@]}"; do
        if [ "$CHECK_MODE" = "ci" ]; then
            matches=$(grep -iE -e "$pattern" "$file" 2>/dev/null | grep -vE "example|sample|template|test|mock|dummy|\\\$\{|\{\{" || true)
        else
            matches=$(git diff --cached "$file" | grep -E "^\+" | grep -iE -e "$pattern" | grep -vE "example|sample|template|test|mock|dummy|\\\$\{|\{\{" || true)
        fi

        if [ -n "$matches" ]; then
            print_error "Potential secret pattern in: $file"
            echo "  Pattern: $pattern"
            echo "$matches" | sed 's/^/  /' | head -3
            break
        fi
    done
done

# =============================================================================
# CHECK 12: SSH Private Keys
# =============================================================================
print_info "Checking for SSH private keys..."

echo "$FILES_TO_CHECK" | while IFS= read -r file; do
    [ -z "$file" ] || [ ! -f "$file" ] && continue

    # Skip security check scripts to avoid false positives
    if echo "$file" | grep -qE "security-check\.sh|pre-commit"; then
        continue
    fi

    if [ "$CHECK_MODE" = "ci" ]; then
        ssh_keys=$(grep "BEGIN.*PRIVATE KEY" "$file" 2>/dev/null || true)
    else
        ssh_keys=$(git diff --cached "$file" | grep -E "^\+" | grep "BEGIN.*PRIVATE KEY" || true)
    fi

    if [ -n "$ssh_keys" ]; then
        print_error "SSH private key found in: $file"
        echo "  → Remove immediately and regenerate the key!"
    fi
done

# =============================================================================
# CHECK 13: Docker Compose Secrets
# =============================================================================
print_info "Checking Docker Compose files for hardcoded secrets..."

DOCKER_FILES=$(echo "$FILES_TO_CHECK" | grep -E "docker-compose.*\.ya?ml$" || true)
if [ -n "$DOCKER_FILES" ]; then
    echo "$DOCKER_FILES" | while IFS= read -r file; do
        [ -z "$file" ] || [ ! -f "$file" ] && continue

        if [ "$CHECK_MODE" = "ci" ]; then
            hardcoded=$(grep -E "(PASSWORD|SECRET|KEY|TOKEN):\s*[^$\{]" "$file" 2>/dev/null | grep -v "example\|sample" || true)
        else
            hardcoded=$(git diff --cached "$file" | grep -E "^\+" | grep -E "(PASSWORD|SECRET|KEY|TOKEN):\s*[^$\{]" | grep -v "example\|sample" || true)
        fi

        if [ -n "$hardcoded" ]; then
            print_warning "Hardcoded credentials in Docker Compose: $file"
            echo "$hardcoded" | sed 's/^/  /'
            echo "  → Use environment variables: \${VAR_NAME}"
        fi
    done
fi

# =============================================================================
# CHECK 14: Gradle/Maven Private Credentials
# =============================================================================
print_info "Checking build files for credentials..."

BUILD_FILES=$(echo "$FILES_TO_CHECK" | grep -E "\.(gradle|kts|xml)$" || true)
if [ -n "$BUILD_FILES" ]; then
    echo "$BUILD_FILES" | while IFS= read -r file; do
        [ -z "$file" ] || [ ! -f "$file" ] && continue

        if [ "$CHECK_MODE" = "ci" ]; then
            creds=$(grep -iE "username|password|credentials" "$file" 2>/dev/null | grep -E "=\s*['\"]" | grep -vE "\\\$|project\.|System\." || true)
        else
            creds=$(git diff --cached "$file" | grep -E "^\+" | grep -iE "username|password|credentials" | grep -E "=\s*['\"]" | grep -vE "\\\$|project\.|System\." || true)
        fi

        if [ -n "$creds" ]; then
            print_warning "Hardcoded credentials in build file: $file"
            echo "$creds" | sed 's/^/  /' | head -3
            echo "  → Use gradle.properties or environment variables"
        fi
    done
fi

# =============================================================================
# CHECK 15: Gitleaks (Optional Tool)
# =============================================================================
if [ "$SKIP_OPTIONAL_TOOLS" = "false" ] && command -v gitleaks &> /dev/null; then
    print_info "Running gitleaks scan..."

    if [ "$CHECK_MODE" = "ci" ]; then
        # CI mode: scan entire repository
        if gitleaks detect --source . --verbose 2>&1 | tee /tmp/gitleaks-output.txt; then
            print_success "Gitleaks: No secrets detected"
        else
            print_error "Gitleaks detected secrets!"
            cat /tmp/gitleaks-output.txt
        fi
    else
        # Hook mode: scan only staged changes
        if gitleaks protect --staged --verbose 2>&1 | tee /tmp/gitleaks-output.txt; then
            print_success "Gitleaks: No secrets detected"
        else
            print_error "Gitleaks detected secrets!"
            cat /tmp/gitleaks-output.txt
        fi
    fi
    rm -f /tmp/gitleaks-output.txt
elif [ "$SKIP_OPTIONAL_TOOLS" = "false" ]; then
    print_info "Skipping gitleaks (not installed)"
fi

# =============================================================================
# CHECK 16: Git Secrets (Optional Tool)
# =============================================================================
if [ "$SKIP_OPTIONAL_TOOLS" = "false" ] && [ "$CHECK_MODE" = "hook" ] && command -v git-secrets &> /dev/null; then
    print_info "Running git-secrets scan..."

    if git secrets --pre_commit_hook -- "$@" 2>&1; then
        print_success "git-secrets: No issues detected"
    else
        print_error "git-secrets detected issues!"
    fi
elif [ "$SKIP_OPTIONAL_TOOLS" = "false" ] && [ "$CHECK_MODE" = "hook" ]; then
    print_info "Skipping git-secrets (not installed)"
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Scan Complete${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $ERRORS_FOUND -gt 0 ]; then
    echo -e "${RED}✗ Found $ERRORS_FOUND error(s)${NC}"
    echo ""
    echo "❌ Security check failed!"
    if [ "$CHECK_MODE" = "hook" ]; then
        echo ""
        echo "To bypass this check (NOT RECOMMENDED):"
        echo "  git commit --no-verify"
    fi
    echo ""
    exit 1
elif [ $WARNINGS_FOUND -gt 0 ]; then
    echo -e "${YELLOW}⚠ Found $WARNINGS_FOUND warning(s)${NC}"
    echo ""
    echo "⚠️  Please review warnings above"
    echo "Proceeding..."
    echo ""
    exit 0
else
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    exit 0
fi