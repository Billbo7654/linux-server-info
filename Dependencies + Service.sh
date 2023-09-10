#!/bin/bash

# Detect the current username
current_user=$(whoami)

# Update and install required packages
sudo apt-get update
sudo apt-get install -y python3 python3-pip lsb-release util-linux ifstat git

# Install Python packages from requirements.txt and handle errors
pip3 install -r requirements.txt || { echo "Error: Failed to install Python requirements."; }

# Clone the GitHub repository
git clone https://github.com/marek-guran/linux-server-info
cd linux-server-info

# Install requirements inside linux-server-info directory
pip3 install -r requirements.txt || { echo "Error: Failed to install Python requirements inside linux-server-info directory."; }

# Create the systemd service file dynamically
cat <<EOL | sudo tee /etc/systemd/system/server-info.service > /dev/null
[Unit]
Description=Linux Server Info
After=network.target

[Service]
WorkingDirectory=/home/$current_user/linux-server-info
ExecStart=/usr/bin/python3 /home/$current_user/linux-server-info/server-info.py
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOL

# Reload and enable/start the service
sudo systemctl daemon-reload
sudo systemctl enable server-info
sudo systemctl start server-info

echo "Python script is running as a service."
echo "Successfully installed."