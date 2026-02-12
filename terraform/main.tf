# Provider configuration
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Data source for Ubuntu AMI
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}


# Security Group
resource "aws_security_group" "app_sg" {
  name        = "${var.environment}-application-sg"
  description = "Security group for application instances"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH"
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP"
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS"
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Spring Boot"
  }

  ingress {
    from_port   = 4200
    to_port     = 4200
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Angular Dev"
  }

  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "MySQL"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.environment}-app-sg"
    Environment = var.environment
  }
}

# Backend Server - NO SOFTWARE INSTALLATION
resource "aws_instance" "backend_server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.backend_instance_type
  key_name      = var.key_name

  security_groups = [aws_security_group.app_sg.name]

  # Minimal user_data - just ensure system is updated
  user_data = <<-EOF
              #!/bin/bash
              apt-get update -y
              EOF

  tags = {
    Name        = "${var.environment}-backend-server"
    Type        = "backend"
    Environment = var.environment
    Role        = "springboot"
  }
}

# Database Server - NO SOFTWARE INSTALLATION
resource "aws_instance" "database_server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.database_instance_type
  key_name      = var.key_name

  security_groups = [aws_security_group.app_sg.name]

  user_data = <<-EOF
              #!/bin/bash
              apt-get update -y
              EOF

  tags = {
    Name        = "${var.environment}-database-server"
    Type        = "database"
    Environment = var.environment
    Role        = "mysql"
  }
}

# Frontend Server - NO SOFTWARE INSTALLATION
resource "aws_instance" "frontend_server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.frontend_instance_type
  key_name      = var.key_name

  security_groups = [aws_security_group.app_sg.name]

  user_data = <<-EOF
              #!/bin/bash
              apt-get update -y
              EOF

  tags = {
    Name        = "${var.environment}-frontend-server"
    Type        = "frontend"
    Environment = var.environment
    Role        = "angular"
  }
}

# S3 Bucket
resource "aws_s3_bucket" "app_bucket" {
  bucket = var.s3_bucket_name

  tags = {
    Name        = var.s3_bucket_name
    Environment = var.environment
  }
}

resource "aws_s3_bucket_versioning" "app_bucket_versioning" {
  bucket = aws_s3_bucket.app_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "app_bucket_pab" {
  bucket = aws_s3_bucket.app_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Generate Ansible inventory dynamically
resource "local_file" "ansible_inventory" {
  content = templatefile("${path.module}/templates/inventory.tpl", {
    backend_ip   = aws_instance.backend_server.public_ip
    database_ip  = aws_instance.database_server.public_ip
    frontend_ip  = aws_instance.frontend_server.public_ip
    key_name     = var.key_name
  })
  filename = "${path.module}/../ansible/inventory/hosts"
}

