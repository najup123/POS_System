import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class Dbconnection {
	
	public static void main (String[] args) throws SQLException
	{
	String sql = "select * from employees where employees_id=5";
	String url="jdbc:mysql://localhost:3306/company_db";
	String username="root";
	String password="password";
	
	
	Connection con = DriverManager.getConnection(url, username, password);
	Statement st = con.createStatement();
	ResultSet rs= st.executeQuery(sql);
	rs.next();
	Array name= rs.getArray(1);
	System.out.println(name);
	con.close();
	}
	
}
