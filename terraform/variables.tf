# Variables
variable "aws_region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "us-east-1"
}

variable "backend_instance_type" {
  description = "Instance type for Backend Spring Boot server"
  type        = string
  default     = "t2.medium"
}

variable "database_instance_type" {
  description = "Instance type for Database server"
  type        = string
  default     = "t2.medium"
}

variable "frontend_instance_type" {
  description = "Instance type for Frontend Angular server"
  type        = string
  default     = "t2.medium"
}

variable "s3_bucket_name" {
  description = "Name for the S3 bucket"
  type        = string
}

variable "key_name" {
  description = "SSH key pair name"
  type        = string
  default     = "my-key-pair"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}
