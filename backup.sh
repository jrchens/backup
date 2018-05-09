#!/bin/sh

# crontab -l
# 30 23 * * * /usr/local/bin/backup.sh

# #################################################################################
DB_FILE="/usr/local/var/backup/jrry.sql.gz"

if [ -f "$DB_FILE" ] ; then
	rm -f "$DB_FILE"
fi
/usr/bin/mysqldump -uroot -h127.0.0.1 --add-locks=false --lock-tables=false --events=true jrry | /bin/gzip > $DB_FILE


# #################################################################################
UPLOAD_FILE_BASE_PATH="/usr/local/var/upload"
UPLOAD_FILE_DIR="jrry"
UPLOAD_BACKUP_FILE="/usr/local/var/backup/jrry.tar"

if [ ! -f "$UPLOAD_BACKUP_FILE" ] ; then
	/bin/tar cPf $UPLOAD_BACKUP_FILE -C $UPLOAD_FILE_BASE_PATH $UPLOAD_FILE_DIR
fi

if [ -f "$UPLOAD_BACKUP_FILE" ] ; then
	/bin/tar uPf $UPLOAD_BACKUP_FILE -C $UPLOAD_FILE_BASE_PATH $UPLOAD_FILE_DIR
fi

/bin/gzip -c $UPLOAD_BACKUP_FILE > "$UPLOAD_BACKUP_FILE".gz


# #################################################################################
OSS_BUCKET_DIR="jrry"

/usr/local/jdk1.7.0_80/bin/java -jar /usr/local/bin/oss-upload.jar $OSS_BUCKET_DIR $DB_FILE "$UPLOAD_BACKUP_FILE".gz

exit 0
