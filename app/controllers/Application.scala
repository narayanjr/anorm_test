package controllers

import play.api._
import play.api.mvc._
import play.api.db._
import play.api.Play.current
import anorm._

class Application extends Controller {
	var db_built = false
	var unique_alias_parser = SqlParser.long("vid") ~ SqlParser.str("vname") ~ SqlParser.long("cid") ~ SqlParser.str("cname") map 
	{
	    case vid ~ vn ~ cid ~ cn => vid + ",\t" + vn + ",\t" + cid + ",\t" + cn
	}
	var index_parser = SqlParser.long(1) ~ SqlParser.str(2) ~ SqlParser.long(3) ~ SqlParser.str(4) map 
	{
	    case vid ~ vn ~ cid ~ cn => vid + ",\t" + vn + ",\t" + cid + ",\t" + cn
	}

	def buildDB(implicit c:java.sql.Connection) = 
	{

		SQL"""DROP TABLE IF EXISTS business""".execute
		SQL"""DROP TABLE IF EXISTS business_connection""".execute

		SQL"""
			CREATE TABLE business (
				business_id int(11) NOT NULL,
				name varchar(50) NOT NULL
			)
		""".execute

		SQL"""
			CREATE TABLE IF NOT EXISTS business_connection (
				vendor_id int(11) NOT NULL,
				client_id int(11) NOT NULL
			)
		""".execute
		SQL"""
			INSERT INTO business (business_id, name)
			VALUES 	(1,  'A'), (2,  'B'), (3,  'C'), (10, 'K'), (11, 'L'), (12, 'M') 
		""".execute

		SQL"""
			INSERT INTO business_connection (vendor_id, client_id)
			VALUES 	(1,  10), (2,  1), (2,  1), (12, 3) 
		""".execute
		db_built = true
	}

  	def index = Action {

	    DB.withConnection {
	    	implicit c=>
	    	buildDB
	    	var html = "<pre>"
	    	html += """The Query:
		    	|SELECT v.business_id AS vid, v.name AS vname, c.business_id AS cid, c.name AS cname
		        |FROM business_connection
		        |JOIN business AS v ON (vendor_id = v.business_id)
		        |JOIN business AS c ON (client_id = c.business_id)
	    		|
	    	|""".stripMargin
	    	html+= "#Expected Results (index parser)</br>"
	    	html+=SQL"""
	    		SELECT v.business_id AS vid, v.name AS vname, c.business_id AS cid, c.name AS cname
		        FROM business_connection
		        JOIN business AS v ON (vendor_id = v.business_id)
		        JOIN business AS c ON (client_id = c.business_id)
	    	""".as(index_parser.*).mkString("","</br>","</br>")
	    	html+="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~</br>"
	    	html+="#unique_alias_parser </br>"
	    	html+=SQL"""
	    		SELECT v.business_id AS vid, v.name AS vname, c.business_id AS cid, c.name AS cname
		        FROM business_connection
		        JOIN business AS v ON (vendor_id = v.business_id)
		        JOIN business AS c ON (client_id = c.business_id)
	    	""".as(unique_alias_parser.*).mkString("","</br>","</br>")
	    	html+="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~</br>"
	    	html+="#unique_alias_parser with fields reversed </br>"
	    	html+= SQL"""
	    		SELECT c.business_id AS cid, c.name AS cname, v.business_id AS vid, v.name AS vname
		        FROM business_connection
		        JOIN business AS v ON (vendor_id = v.business_id)
		        JOIN business AS c ON (client_id = c.business_id)
	    	""".as(unique_alias_parser.*).mkString("","</br>","</br>")
	    	html+="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~</br>"

	    	html+= 
	    	"""
	    		|Table Structure:
	    		|
	    		|business:
	    		|+-------------+-------------+------+-----+---------+-------+
				|| Field       | Type        | Null | Key | Default | Extra |
				|+-------------+-------------+------+-----+---------+-------+
				|| business_id | int(11)     | NO   |     | NULL    |       |
				|| name        | varchar(50) | NO   |     | NULL    |       |
				|+-------------+-------------+------+-----+---------+-------+
				|
				|business_connection:
				|+-----------+---------+------+-----+---------+-------+
				|| Field     | Type    | Null | Key | Default | Extra |
				|+-----------+---------+------+-----+---------+-------+
				|| vendor_id | int(11) | NO   |     | NULL    |       |
				|| client_id | int(11) | NO   |     | NULL    |       |
				|+-----------+---------+------+-----+---------+-------+
				|
				|Table Contents:
				|
				|business:
				|+-------------+------+
				|| business_id | name |
				|+-------------+------+
				||           1 | A    |
				||           2 | B    |
				||           3 | C    |
				||          10 | K    |
				||          11 | L    |
				||          12 | M    |
				|+-------------+------+
				|6 rows in set (0.00 sec)
				|
				|business_connection:
				|+-----------+-----------+
				|| vendor_id | client_id |
				|+-----------+-----------+
				||         1 |        10 |
				||         2 |         1 |
				||         2 |         1 |
				||        12 |         3 |
				|+-----------+-----------+
				|4 rows in set (0.00 sec)
	    	""".stripMargin

	    	html+= "</pre>"
	    	Ok(html).as("text/html");
	    }	

	    
  	}
}
