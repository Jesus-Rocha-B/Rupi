import pymysql

# Django exige mysqlclient 2.2.1 o superior. PyMySQL no necesita compilar contra MySQL y ofrece la
# misma interfaz, así que se presenta como esa versión para no depender de cabeceras de C en Windows.
pymysql.version_info = (2, 2, 1, 'final', 0)
pymysql.install_as_MySQLdb()

# TiDB Cloud Serverless y MySQL: omitir la validación estricta de versión mínima de Django
from django.db.backends.base.base import BaseDatabaseWrapper
BaseDatabaseWrapper.check_database_version_supported = lambda self: None
