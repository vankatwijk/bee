package bee.happy.inholland.nl.commands;


import java.sql.SQLException;
import java.util.List;

import sun.org.mozilla.javascript.internal.Token.CommentType;

import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

public class DBCommandExecuter {
	//TODO: get the connection string from config file
    final String databaseUrl = "jdbc:postgresql://localhost:5432/BeeHappy";
	ConnectionSource connectionSource; //connection source to our database
	Gson gson;
	
	public DBCommandExecuter() {
		gson = new Gson();
		try {
			connectionSource = new JdbcConnectionSource(databaseUrl);
	        //TODO: get the password from config file?
			((JdbcConnectionSource)connectionSource).setUsername("postgres");
			((JdbcConnectionSource)connectionSource).setPassword("beeHappy");
		} catch (SQLException e) {
			System.out.println("e.getMessage() " + e.getMessage());
			System.out.println("e.getClass() " + e.getClass());
			System.out.println("e.getErrorCode() " + e.getErrorCode());
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public BeeCommandResult create(CreateCommand command) {
		BeeCommandResult result = null;
		
		System.out.println("adding to DB:");
		
		try {
			Class objectClass = getObjectClass(command.getClassName());
			Object object = gson.fromJson(command.getObjectJson(), objectClass);
			System.out.println("object to be inserted: " + object);
			
			// instantiate the dao
			Dao<? super Object, Integer> objectClassDao = DaoManager.createDao(connectionSource, objectClass);
			
			//create object in DB
			int nInsertedRows = objectClassDao.create(objectClass.cast(object));

			if(nInsertedRows==1){
				String objectJson = gson.toJson(object);
				//object was successfuly added to DB
				result = new CreateCommandResult(command.getClassName(), objectJson);
			} else if(nInsertedRows == 0){
				throw new Exception(object + " could not be added in DB");
			}
			else{
				throw new Exception("Something went wrong...");
			}
			
		} catch (SQLException e) {
			result = errorResultFromException(e, command.getCommandType());
			e.printStackTrace();
		} catch (Exception e){
			result = errorResultFromException(e, command.getCommandType());
			e.printStackTrace();			
		}
		
		return result;
		
	}
	
	public BeeCommandResult update(UpdateCommand command) {
		BeeCommandResult result = null;
		
		System.out.println("updating in DB:");
		
		try {
			Class objectClass = getObjectClass(command.getClassName());
			Object object = gson.fromJson(command.getObjectJson(), objectClass);
			System.out.println("object to be updated: " + object);
			
			// instantiate the dao
			Dao<? super Object, Integer> objectClassDao = DaoManager.createDao(connectionSource, objectClass);
			
			//update the object in DB
			int nUpdatedRows = objectClassDao.update(object);			
			if(nUpdatedRows==1){
				String objectJson = gson.toJson(object);
				//object was successfuly updated
				result = new UpdateCommandResult(command.getClassName(), objectJson);
			} else if(nUpdatedRows == 0){
				throw new Exception(object + " not found in DB");
			}
			else{
				throw new Exception("Something went wrong...");
			}
			
		} catch (SQLException e) {
			result = errorResultFromException(e, command.getCommandType());
			e.printStackTrace();
		} catch (Exception e){
			result = errorResultFromException(e, command.getCommandType());
			e.printStackTrace();			
		}
		
		return result;
		
	}

	public BeeCommandResult delete(DeleteCommand command) {
		BeeCommandResult result = null;
		System.out.println("deleting in DB:");
		
		try {
			Class objectClass = getObjectClass(command.getClassName());
			Object object = gson.fromJson(command.getObjectJson(), objectClass);
			System.out.println("object to be deleted: " + object);
			// instantiate the dao
			Dao<? super Object, Integer> objectClassDao = DaoManager.createDao(connectionSource, objectClass);
			
			//delete the object in DB
			int nDeletedRows = objectClassDao.delete(object);	
			if(nDeletedRows==1){
				String objectJson = gson.toJson(object);
				//object was successfuly deleted
				result = new DeleteCommandResult(command.getClassName(), objectJson);
			} else if(nDeletedRows == 0){
				throw new Exception(object + " not found in DB");
			}
			else{
				throw new Exception("Something went wrong...");
			}
			
		} catch (SQLException e) {
			result = errorResultFromException(e, command.getCommandType());
			e.printStackTrace();
		} catch (Exception e){
			result = errorResultFromException(e, command.getCommandType());
			e.printStackTrace();			
		}
		
		
		return result;
	}
	
	
	public Class getObjectClass(String className){
		Class objectClass = null;
		try {
			objectClass = Class.forName(className);
			
		} catch (ClassNotFoundException e) {			
			//TODO: implement proper logging
			e.printStackTrace();
		}
		return objectClass;		
	}
	
	
	public int selectAll(){
		Class objectClass = getObjectClass("bee.happy.inholland.nl.domainmodel.Yard");
		
		// instantiate the dao
		try {
			Dao<? super Object, Integer> objectClassDao = DaoManager.createDao(connectionSource, objectClass);
			List<?> list = objectClassDao.queryForAll();
			for(Object li : list){
				System.out.println("AAA: "+li);
			}
			System.out.println("list" + list.getClass());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	public ErrorResult errorResultFromException(Exception e, BeeCommandType commandType){
		String exceptionClassName = e.getClass().getName();
		String exceptionMessage = e.getMessage();
		return new ErrorResult(commandType, exceptionClassName, exceptionMessage);
	}
}
