#!/bin/bash
mysqld_safe --skip-grant-tables &
sleep 3
mysql -u root -e "UPDATE mysql.user SET authentication_string=PASSWORD('123456') WHERE User='root'; FLUSH PRIVILEGES;"
killall mysqld_safe
