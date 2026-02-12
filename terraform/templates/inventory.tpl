[backend]
backend ansible_host=${backend_ip}

[database]
database ansible_host=${database_ip}

[frontend]
frontend ansible_host=${frontend_ip}

[all:vars]
ansible_user=ubuntu
ansible_ssh_private_key_file=~/.ssh/${key_name}
ansible_python_interpreter=/usr/bin/python3
