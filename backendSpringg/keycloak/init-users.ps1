# ============================================================
#  Script: Création des utilisateurs Keycloak pour Stuffing
# ============================================================

$keycloakUrl = "http://localhost:8180"
$realm = "stuffing"

# 1. Token admin
$body = @{ grant_type="password"; client_id="admin-cli"; username="admin"; password="admin" }
$at = (Invoke-RestMethod -Uri "$keycloakUrl/realms/master/protocol/openid-connect/token" -Method POST -Body $body).access_token
$h = @{ Authorization="Bearer $at"; "Content-Type"="application/json" }

# 2. Fonction pour creer un user et lui assigner un role
function Create-KcUser($username, $email, $firstName, $lastName, $password, $roleName) {
    $userBody = @{
        username = $username
        email = $email
        firstName = $firstName
        lastName = $lastName
        enabled = $true
        emailVerified = $true
        credentials = @(@{ type="password"; value=$password; temporary=$false })
    } | ConvertTo-Json -Depth 3

    try {
        Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/users" -Method POST -Headers $h -Body $userBody
        Write-Host "[OK] User cree: $email"
    } catch {
        Write-Host "[SKIP] User $email existe deja ou erreur"
    }

    # Recuperer l'ID du user
    $users = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/users?email=$email" -Headers $h
    if ($users.Count -eq 0) { Write-Host "[ERR] User $email introuvable"; return }
    $userId = $users[0].id

    # Recuperer la representation du role
    $role = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/roles/$roleName" -Headers $h

    # Assigner le role
    $roleBody = "[" + ($role | ConvertTo-Json -Compress) + "]"
    try {
        Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/users/$userId/role-mappings/realm" -Method POST -Headers $h -Body $roleBody
        Write-Host "[OK] Role $roleName assigne a $email"
    } catch {
        Write-Host "[SKIP] Role $roleName deja assigne a $email"
    }
}

# 3. Creer les utilisateurs
Write-Host "`n=== Creation des utilisateurs Keycloak ===`n"

Create-KcUser "admin@soprahr.com" "admin@soprahr.com" "Admin" "Stuffing" "Admin1234!" "ADMIN"
Create-KcUser "karim.benali@soprahr.com" "karim.benali@soprahr.com" "Karim" "Benali" "Manager1234!" "DELIVERY_MANAGER"
Create-KcUser "yassine.elamrani@soprahr.com" "yassine.elamrani@soprahr.com" "Yassine" "El Amrani" "Collab1234!" "COLLABORATEUR"
Create-KcUser "fatima.zahra@soprahr.com" "fatima.zahra@soprahr.com" "Fatima" "Zahra" "Collab1234!" "COLLABORATEUR"
Create-KcUser "omar.tazi@soprahr.com" "omar.tazi@soprahr.com" "Omar" "Tazi" "Collab1234!" "COLLABORATEUR"
Create-KcUser "sara.benmoussa@soprahr.com" "sara.benmoussa@soprahr.com" "Sara" "Benmoussa" "Collab1234!" "COLLABORATEUR"
Create-KcUser "amine.cherkaoui@soprahr.com" "amine.cherkaoui@soprahr.com" "Amine" "Cherkaoui" "Collab1234!" "COLLABORATEUR"
Create-KcUser "nadia.idrissi@soprahr.com" "nadia.idrissi@soprahr.com" "Nadia" "Idrissi" "Collab1234!" "COLLABORATEUR"
Create-KcUser "mehdi.alaoui@soprahr.com" "mehdi.alaoui@soprahr.com" "Mehdi" "Alaoui" "Collab1234!" "COLLABORATEUR"
Create-KcUser "leila.fassi@soprahr.com" "leila.fassi@soprahr.com" "Leila" "Fassi" "Collab1234!" "COLLABORATEUR"

Write-Host "`n=== TERMINE ! ==="

