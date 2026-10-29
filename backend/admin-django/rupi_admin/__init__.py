import pymysql

# Django exige mysqlclient 2.2.1 o superior. PyMySQL no necesita compilar contra MySQL y ofrece la
# misma interfaz, así que se presenta como esa versión para no depender de cabeceras de C en Windows.
pymysql.version_info = (2, 2, 1, 'final', 0)
pymysql.install_as_MySQLdb()
