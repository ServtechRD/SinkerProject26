# Acceptance Criteria - X006

## Preflight (OPTIONS) must succeed for remote origin
Run on the VM:

```bash
curl -i -X OPTIONS "http://localhost:8080/api/auth/login" \
  -H "Origin: http://<VM-IP>:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type,authorization"

# Acceptance Criteria - X006

## Preflight (OPTIONS) must succeed for remote origin
Run on the VM:

```bash
curl -i -X OPTIONS "http://localhost:8080/api/auth/login" \
  -H "Origin: http://<VM-IP>:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type,authorization"
