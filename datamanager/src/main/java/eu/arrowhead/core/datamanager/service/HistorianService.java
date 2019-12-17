package eu.arrowhead.core.datamanager.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.dto.shared.SenML;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.*; 
import java.util.Vector;
import java.util.Properties;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.google.gson.Gson;


@Service
public class HistorianService {
	 private static Connection connection = null;
	 private static String dbAddress;
	 private static String dbUser;
	 private static String dbPassword;
	 private static Properties props = null;

	 static {

	}

	/**
	 * @fn static boolean Init(Properties props)
	 * @brief 
	 *
	 */
	public static boolean Init(Properties propss){
	  props = propss;

	  System.out.println("HistorianServce::Init()");

	  try {
	    Class.forName("com.mysql.jdbc.Driver");
	  } catch (ClassNotFoundException e) {
	    System.out.println("Where is your MySQL JDBC Driver?");
	    e.printStackTrace();
	    return false;
	  }

	  System.out.println("MySQL JDBC Driver Registered!");
	  try {
	    connection = getConnection();
	    //checkTables(connection, props.getProperty("spring.datasource.database"));
	    connection.close();
	  } catch (SQLException e) {
	    System.out.println("Connection Failed! Check output console");
	    e.printStackTrace();
	    System.exit(-1);
	    return false;
	  }

	  return true;
	}


	/**
	 * @fn private static Connection getConnection()
	 * @brief 
	 *
	 */
	private static Connection getConnection() throws SQLException {
	  Connection conn = DriverManager.getConnection(props.getProperty("spring.datasource.url"), props.getProperty("spring.datasource.username"), props.getProperty("spring.datasource.password"));

	  return conn;
	}


	/**
	 * @fn private static void closeConnection(Connection conn)
	 * @brief 
	 *
	 */
	private static void closeConnection(Connection conn) throws SQLException {
	  conn.close();
	}


	/**
	 * @fn static int serviceToID(String serviceName, Connection conn)
	 * @brief Returns the database ID of a specific service
	 *
	 */
	static int serviceToID(String serviceName, Connection conn) {
	  int id=-1;

	  //System.out.println("serviceToID('"+serviceName+"')");
	  Statement stmt = null;
	  try {
	    stmt = conn.createStatement();
	    String sql;
	    sql = "SELECT id FROM dmhist_services WHERE service_name='"+serviceName+"' LIMIT 1;";
	    ResultSet rs = stmt.executeQuery(sql);

	    rs.next();
	    id  = rs.getInt("id");

	    rs.close();
	    stmt.close();
	  }catch(SQLException se){
	    id = -1;
	    //se.printStackTrace();
	  }catch(Exception e){
	    id = -1;
	    //e.printStackTrace();
	  }

	  //System.out.println("serviceToID('"+serviceName+"')="+id);
	  return id;
	}


	/**
	 * @fn static ArrayList<String> getSystems()
	 *
	 */
	public static ArrayList<String> getSystems(){
	  ArrayList<String> ret = new ArrayList<String>();
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    Statement stmt = conn.createStatement();
	    String sql = "SELECT DISTINCT(service_name) FROM dmhist_services;";

	    ResultSet rs = stmt.executeQuery(sql);
	    while(rs.next() == true) {
	      ret.add(rs.getString(1));
	    }
	    rs.close();
	    stmt.close();
	  } catch (SQLException e) {
	    System.err.println(e.toString());
	  } finally {
	    try {
	      closeConnection(conn);
	    } catch (SQLException e) {}

	  }

	  return ret;
	}


	/**
	 * @fn 
	 *
	 */
	public static boolean addServiceForSystem(String systemName, String serviceName, String serviceType){
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(serviceName, conn);
	    //System.out.println("addServiceForSystem: found " + id);
	    if (id != -1) {
	      return false; //already exists
	    } else {
	      Statement stmt = conn.createStatement();
	      String sql = "INSERT INTO dmhist_services(system_name, service_name, service_type) VALUES(\""+systemName+"\", \""+serviceName+"\", \""+serviceType+"\");"; //bug: check name for SQL injection!
	      //System.out.println(sql);
	      int mid = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
	      ResultSet rs = stmt.getGeneratedKeys();
	      rs.next();
	      id = rs.getInt(1);
	      rs.close();
	      stmt.close();
	      //System.out.println("addServiceForSystem: created " + id);

	    }

	  } catch (SQLException e) {
	    return false;
	  } finally {
	    try {
	      closeConnection(conn);
	    } catch (SQLException e) {}
	  
	  }

	  return true;
	}


	/**
	 * @fn
	 *
	 */
	public static ArrayList<String> getServicesFromSystem(String systemName){
	  ArrayList<String> ret = new ArrayList<String>();
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    Statement stmt = conn.createStatement();
	    String sql = "SELECT DISTINCT(service_name) FROM dmhist_services WHERE system_name='"+systemName+"';";
	    //System.out.println(sql);

	    ResultSet rs = stmt.executeQuery(sql);
	    while(rs.next() == true) {
	      //System.out.println("---"+rs.getString(1));
	      ret.add(rs.getString(1));
	    }
	    rs.close();
	    stmt.close();
	  }catch(SQLException db){
	    //System.out.println(db.toString());
	  } finally {
	    try {
	      connection.close();
	    }catch(SQLException db){}

	  }


	  return ret;
	}


	/**
	 * @fn static boolean createEndpoint(String name)
	 *
	 */
	public static boolean createEndpoint(String systemName, String serviceName) {
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(serviceName, conn);
	    //System.out.println("createEndpoint: found " + id);
	    if (id != -1) {
	      return true; //already exists
	    } else {
	      Statement stmt = conn.createStatement();
	      String sql = "INSERT INTO dmhist_services(system_name, service_name) VALUES(\""+systemName+"\", \""+serviceName+"\");"; //bug: check name for SQL injection!
	      //System.out.println(sql);
	      int mid = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
	      ResultSet rs = stmt.getGeneratedKeys();
	      rs.next();
	      id = rs.getInt(1);
	      rs.close();
	      stmt.close();
	      //System.out.println("createEndpoint: created " + id);

	    }

	  } catch (SQLException e) {
	    //System.out.println("createEndpoint:: "+e.toString());
	    return false;
	  } finally {
	    try{
	      closeConnection(conn);
	    } catch(Exception e){}
	  
	  }

	  return true;
	}


	/**
	 * @fn static boolean updateEndpoint(String name, Vector<SenML> msg)
	 *
	 */
	public static boolean updateEndpoint(String name, Vector<SenML> msg) {
	  boolean ret = false;

	  double maxTs = maxTs(msg);
	  double minTs = minTs(msg);
	  System.out.println("bt(msg): "+(msg.get(0).getBt())+", minTs(msg): "+minTs+", maxTs(msg): " + maxTs);

	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(name, conn);
	    if (id != -1) {
	      Statement stmt = conn.createStatement();
	      String sql = "INSERT INTO dmhist_messages(sid, bt, mint, maxt, msg, datastored) VALUES("+id+", "+msg.get(0).getBt()+","+minTs+", "+maxTs+", '"+msg.toString()+"',NOW());"; //how to escape "
	      System.out.println(sql);
	      int mid = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
	      ResultSet rs = stmt.getGeneratedKeys();
	      rs.next();
	      mid = rs.getInt(1);
	      rs.close();
	      stmt.close();

	      // that was the entire message, now insert each individual JSON object in the message
	      for (SenML m : msg) {
		sql = "INSERT INTO dmhist_objects(mid, t, obj) VALUES("+mid+", "+m.getT()+",´"+m.toString()+"');"; //how to escape "
		System.out.println(sql);

	      }

	    } else {
	    }
	  } catch (SQLException e) {
	    //System.out.println(e.toString());
	    ret = false;
	  } finally {
	    try{
	      closeConnection(conn);
	    } catch(Exception e){}
	  
	  }

	  return ret;
	}


	/**
	 * @fn static Vector<SenML> fetchEndpoint(String name, int count, Vector<String> signals)
	 *
	 */
	public static Vector<SenML> fetchEndpoint(String name, long ts, String tsop, int count, Vector<String> signals) {
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(name, conn);
	    //System.out.println("Got id of: " + id);
	    String signalss = "";
	    for (String sig: signals) {
	      signalss += ("'"+sig + "',");
	    }
	    signalss = signalss.substring(0, signalss.length()-1); //remove last ',' XXX. remove/detect escape characters 
	    System.out.println("Signals: '" + signalss + "'");

	    if (id != -1) {
	      Statement stmt = conn.createStatement();

	      String sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" ORDER BY datastored DESC;";
	      if (ts != -1 && tsop != null) {
		switch(tsop){
		  case "ge":
		    sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" AND datastored >= "+ts+" ORDER BY datastored DESC;";

		    break;
		  case "eq":
		    sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" AND datastored="+ts+" ORDER BY datastored DESC;";
		    break;
		}
	      }
	      //String sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" ORDER BY datastored DESC;";
	      System.out.println(sql);
	      ResultSet rs = stmt.executeQuery(sql);

	      Vector<SenML> messages = new Vector<SenML>();
	      SenML hdr = new SenML();
	      hdr.setBn(name);
	      messages.add(hdr);
	      double bt = 0;
	      String bu = null;
	      while(rs.next() == true && count > 0) {
		Gson gson = new Gson();
		SenML[] smlarr = gson.fromJson(rs.getString("msg"), SenML[].class);
		System.out.println("fetch() " + rs.getString("msg"));

		for (SenML m : smlarr) {
		  if (m.getBt() != null) {
		    bt = m.getBt();

		    if (((SenML)messages.firstElement()).getBt() == null)
		      ((SenML)messages.firstElement()).setBt(bt);
		  }
		  if (m.getBu() != null) {
		    bu = m.getBu();

		    if (((SenML)messages.firstElement()).getBu() == null)
		      ((SenML)messages.firstElement()).setBu(bu);
		  }

		  System.out.println("  got " + m.getN());
		  // check if m contains a value in signals
		  if (signals.contains(m.getN())) {
		    if (m.getT() != null) {
		      if (m.getT() < 268435456) // if less than 2**28, it is relative
		        m.setT(bt+m.getT());
		    } else {
		      m.setT(bt);
		    }
		    messages.add(m);
		    count--;
		  }
		}
	      }

	      rs.close();
	      stmt.close();

	      //update bn fields (i.e. remove if the same as the first
	      String startbn = ((SenML)messages.firstElement()).getBn();
	      for (int i = 1; i< messages.size(); i++) {
		SenML m = (SenML)messages.get(i);
		System.out.println("startbn: "+ startbn+"\tm.Bn: "+m.getBn());
		if (startbn.equals(m.getBn()))
		  m.setBn(null);
	      }

	      //recalculate a bt time and update all relative timestamps
	      Double startbt = ((SenML)messages.firstElement()).getBt();
	      for (SenML m : messages) {
		  System.out.println("\t" + m.toString());
		  if (m.getT() != null)
		    m.setT(m.getT()-startbt);
	      }

	      // update unit tags
	      String startbu = ((SenML)messages.firstElement()).getBu();
	      if (startbu != null) {
		for (SenML m : messages) {
		  try {
		    if (m.getU().equals(startbu)){
		      m.setU(null);
		    }
		  } catch(Exception e){}
		}
	      }

	      return messages;
	    } 
	  } catch (SQLException e) {
	    System.err.println(e.toString());
	  } finally {
	    try{
	      closeConnection(conn);
	    } catch(Exception e){}
	  
	  }

	  return null;
	}


	/**
	 * @fn static Vector<SenML> fetchEndpoint(String name, int count)
	 * @brief
	 * @param name
	 * @param count
	 * @return
	 */
	public static Vector<SenML> fetchEndpoint(String name, long ts, String tsop, int count) {
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(name, conn);
	    //System.out.println("Got id of: " + id);
	    if (id != -1) {
	      Statement stmt = conn.createStatement();
	      String sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" ORDER BY datastored DESC;";
	      if (ts != -1 && tsop != null) {
		switch(tsop){
		  case "ge":
		    sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" AND datastored >= "+ts+" ORDER BY datastored DESC;";
		    break;
		  case "eq":
		    sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" AND datastored="+ts+" ORDER BY datastored DESC;";
		    break;
		}
	      }
	      //String sql = "SELECT * FROM dmhist_messages WHERE sid="+id+" ORDER BY datastored DESC;";
	      System.out.println(sql);
	      ResultSet rs = stmt.executeQuery(sql);

	      String msg = "";
	      Vector<SenML> messages = new Vector<SenML>(); 
	      while(rs.next() == true && count > 0) {
		msg = rs.getString("msg");
		//System.out.println("###\n"+ msg + "###");
		Gson gson = new Gson();
		SenML[] smlarr = gson.fromJson(msg, SenML[].class);

		//System.out.println("fetch() " + msg);
		for (SenML m : smlarr) {
		  //if (m.getT() == null)
		    //m.setT(sm.getBt()); //System.out.println("bt is NULL" );

		  messages.add(m);
		  count--;
		}
	      }

	      rs.close();
	      stmt.close();

	      if (messages.size() > 0) {

		//update bn fields (i.e. remove if the same as the first
		String startbn = ((SenML)messages.firstElement()).getBn();
		for (int i = 1; i< messages.size(); i++) {
		  SenML m = (SenML)messages.get(i);
		  //System.out.println("startbn: "+ startbn+"\tm.Bn: "+m.getBn());
		  if (startbn.equals(m.getBn()))
		    m.setBn(null);
		}

		//recalculate a bt time and update all relative timestamps
		Double startbt = ((SenML)messages.firstElement()).getBt();
		for (SenML m : messages) {
		  //System.out.println("\t" + m.toString());
		  if (m.getT() != null)
		    m.setT(m.getT()-startbt);
		}

		// update unit tags
		String startbu = ((SenML)messages.firstElement()).getBu();
		if (startbu != null) {
		  for (SenML m : messages) {
		    try {
		      if (m.getU().equals(startbu)){
			m.setU(null);
		      }
		    } catch(Exception e){}
		  }
		}

	      }

	      return messages;

	    } 
	  } catch (SQLException e) {
	    //System.err.println(e.toString());
	  } finally {
	    try{
	      closeConnection(conn);
	    } catch(Exception e){}

	  }

	  return null;
	}

	//returns largest (newest) timestamp value
	private static double maxTs(Vector<SenML> msg) {
	  double bt = msg.get(0).getBt();
	  double max = bt;
	  for (SenML m : msg) {

	    if (m.getT() != null) {
	      if ((m.getT() + bt) > max )
		max = m.getT() + bt;
	    }
	  }

	  return max;
	}

	//returns smallest (oldest) timestamp value
	private static double minTs(Vector<SenML> msg) {
	  double bt = msg.get(0).getBt();
	  double min = bt;
	  for (SenML m : msg) {

	    if (m.getT() != null) {
	      if ((m.getT() + bt) < min )
		min = m.getT() + bt;
	    }
	  }

	  return min;
	}
}
