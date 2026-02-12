# Outputs
output "backend_ip" {
  value = aws_instance.backend_server.public_ip
}

output "database_ip" {
  value = aws_instance.database_server.public_ip
}

output "frontend_ip" {
  value = aws_instance.frontend_server.public_ip
}

output "s3_bucket_name" {
  value = aws_s3_bucket.app_bucket.id
}
