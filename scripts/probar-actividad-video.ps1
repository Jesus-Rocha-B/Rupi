$ErrorActionPreference = 'Stop'
$headers = @{ Origin = "http://localhost:5173" }
$login = Invoke-RestMethod -Uri 'http://127.0.0.1:8081/api/v1/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"estudiante.demo","password":"123456"}' -Headers $headers -SessionVariable s

# Buscar catálogo
$catalog = Invoke-RestMethod -Uri 'http://127.0.0.1:8081/api/v1/student/learning-routes/search?limit=5' -Method Get -Headers $headers -WebSession $s
$target = $catalog.routes[0]
Write-Host "Target route: $($target.routeTitle) (id: $($target.versionRouteId))"

# Inscribir
$detail = Invoke-RestMethod -Uri "http://127.0.0.1:8081/api/v1/student/learning-routes/$($target.versionRouteId)/enroll" -Method Post -Headers $headers -WebSession $s
Write-Host "Enrolled successfully in: $($detail.title)"

$node1 = $detail.nodes[0]
Write-Host "Opening node 1: $($node1.title) (id: $($node1.id))"
$activity = Invoke-RestMethod -Uri "http://127.0.0.1:8081/api/v1/student/learning-routes/$($target.versionRouteId)/nodes/$($node1.id)/start" -Method Post -Headers $headers -WebSession $s
Write-Host "Activity title: $($activity.title)"
Write-Host "Total blocks returned: $($activity.blocks.Count)"
foreach ($b in $activity.blocks) {
    Write-Host "  -> Block: type=$($b.type) | url=$($b.url) | text=$($b.text)"
}
