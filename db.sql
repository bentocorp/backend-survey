CREATE DATABASE survey;
CREATE USER 'survey'@'localhost' IDENTIFIED BY 'survey';
GRANT ALL PRIVILEGES ON survey.* TO 'survey'@'localhost';
