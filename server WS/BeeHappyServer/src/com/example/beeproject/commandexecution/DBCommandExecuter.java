package com.example.beeproject.commandexecution;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;


import com.example.beeproject.commandexecution.commands.*;
import com.example.beeproject.commandexecution.results.*;
import com.example.beeproject.db.ConnectionProvider;
import com.example.beeproject.global.classes.*;
import com.example.beeproject.gsonconvertion.GsonProvider;
import com.example.beeproject.syncing.DeletedObject;
import com.example.beeproject.utils.PropertiesProvider;

public class DBCommandExecuter {
	
	Gson gson;
	
	
	public DBCommandExecuter() {
		gson = GsonProvider.getGson();
	}
	
	
	public BeeCommandResult create(CreateCommand command) {
		BeeCommandResult result = null;
		
		System.out.println("adding to DB:");
		
		try {
			Class objectClass = getObjectClass(command.getClassName());
			BeeObjectInterface object = gson.fromJson(command.getObjectJson(), objectClass);
			System.out.println("object to be inserted: " + object);
			
			// instantiate the dao
			Dao<? super Object, Integer> objectClassDao = DaoManager.createDao(ConnectionProvider.getConnectionSource(), objectClass);
			
			//create object in DB
			int nInsertedRows = objectClassDao.create(objectClass.cast(object));
	
			if(nInsertedRows==1){
				String objectJson = gson.toJson(object, BeeObjectInterface.class);
				//object was successfuly added to DB
				result = new CreateCommandResult(command.getClassName(), objectJson);
			} else if(nInsertedRows == 0){
				throw new Exception( "Object could not be added in DB", new Throwable("Adding " + object.toString()));
			}
			else{
				throw new Exception("Something went wrong...", new Throwable("Adding " + object.toString()));
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
	
	/**
	 * Executes the UpdateCommand<br>
	 * ATTENTION: 
	 * Command contains object of BeeObjectInterface that needs to be updated.
	 * The id of this object is the server side id, no further processing needed.
	 * @param command
	 * @return
	 */
	public BeeCommandResult update(UpdateCommand command) {
		BeeCommandResult result = null;
		
		System.out.println("updating in DB:");
		
		try {
			Class objectClass = getObjectClass(command.getClassName());
			BeeObjectInterface object = gson.fromJson(command.getObjectJson(), objectClass);
			System.out.println("object to be updated: " + object);
			
			// instantiate the dao
			Dao<? super Object, Integer> objectClassDao = DaoManager.createDao(ConnectionProvider.getConnectionSource(), objectClass);
			
			BeeObjectInterface objectInDb = (BeeObjectInterface) objectClassDao.queryForId(object.getId());
			
			if(objectInDb==null){
				//object not found in DB
				throw new Exception( "Object could not be found in DB", new Throwable("Updating " + object.toString()));
			}
			else{
				//update the object in DB
				System.out.println("object to be updated: " + object);
				
				int nUpdatedRows = objectClassDao.update(object);			
				if(nUpdatedRows==1){
					String objectJson = gson.toJson(object, BeeObjectInterface.class);
					//object was successfuly updated
					result = new UpdateCommandResult(command.getClassName(), objectJson);
				} else if(nUpdatedRows == 0){
					throw new Exception( "Object could not be updated in DB", new Throwable("Updating " + object.toString()));
				}
				else{
					throw new Exception("Something went wrong...", new Throwable("Updating " + object.toString()));
				}
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
	
	/**
	 * Executes the DeleteCommand
	 * <p>DeleteCommand contains json of DeletedObject, which contains info on the object to be deleted.
	 * <p>In order to keep data for later data mining, the objects are not actually being deleted,
	 * just marked with deleted==true
	 * @param command
	 * @return
	 */
	public BeeCommandResult markAsDeleted(DeleteCommand command) {
		BeeCommandResult result = null;
		System.out.println("Marking as deleted in DB:");
		
		try {
			Class objectFromJsonClass = getObjectClass(command.getClassName());
			DeletedObject deletedObjectInfo = gson.fromJson(command.getObjectJson(), objectFromJsonClass);
			
			Class objectClass = getObjectClass(deletedObjectInfo.getObjectClassName()); //class of an object that needs to be deleted
			BeeObjectInterface objectToDelete = (BeeObjectInterface) objectClass.newInstance();
			objectToDelete.setId(deletedObjectInfo.getServerSideId());
			
			System.out.println("object to be marked as deleted: " + objectToDelete);
			// instantiate the dao
			Dao<? super BeeObjectInterface, Integer> objectClassDao = DaoManager.createDao(ConnectionProvider.getConnectionSource(), objectClass);
			BeeObjectInterface objectInDb = (BeeObjectInterface) objectClassDao.queryForId(objectToDelete.getId());
			
			if(objectInDb==null){
				//object not found in DB
				throw new Exception( "Object could not be found in DB", new Throwable("Marking as deleted " + objectToDelete.toString()));
			}
			else{
				/* 
				 * Marking the object as deleted - set deleted=true and update
				 * */
				objectInDb.setDeleted(true);
				int nUpdatedObjects = cascadeMarkAsDeleted(objectInDb);
				int nUpdatedRows = objectClassDao.update(objectInDb);	
				if(nUpdatedRows==1){
					String objectJson = gson.toJson(deletedObjectInfo, DeletedObject.class);
					//object was successfuly marked as deleted
					result = new DeleteCommandResult(DeletedObject.class.getName(), objectJson);
				} else if(nUpdatedRows == 0){
					throw new Exception( "Object could not be marked as deleted in DB", new Throwable("Marking as deleted " + objectToDelete.toString()));
				}
				else{
					throw new Exception("Something went wrong...", new Throwable("Marking as deleted " + objectToDelete.toString()));
				}
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
	
	
	/**
	 * Method to propagate marked as deleted through child objects.
	 * <p>F.e. HiveObject is child of YardObject, so after marking YardObject as deleted, 
	 * all related HiveObjects must be marked as deleted too.
	 * <p>See Diagrams/DomainObjects.ucls for a diagram of domain objects and their relations
	 * @param objectInDb
	 * @return nr of child objects affected by the cascade
	 * @throws SQLException 
	 */
	private int cascadeMarkAsDeleted(BeeObjectInterface parentObject) throws SQLException {
		int nrAffected = 0;
		System.out.println("cascadeMarkAsDeleted: " + parentObject);
		
		List<BeeObjectInterface> childrenToMarkDeleted;
		try {
			childrenToMarkDeleted = parentObject.listChildRelations();
			System.out.println("To mark deleted: \n" + childrenToMarkDeleted);
			/* 
			 * Marking the objects as deleted - set deleted=true and update
			 * */
			for(BeeObjectInterface objToMarkDeleted : childrenToMarkDeleted){
				Dao<? super BeeObjectInterface, Integer> childObjectClassDao = (Dao<? super BeeObjectInterface, Integer>) DaoManager.createDao(ConnectionProvider.getConnectionSource(), objToMarkDeleted.getClass());
				//have to update the object fetched from DB by id, otherwise some fields get set to default values
				BeeObjectInterface objToMarkInDB = (BeeObjectInterface) childObjectClassDao.queryForId(objToMarkDeleted.getId());
				objToMarkInDB.setDeleted(true);
				int nUpdateChildren = cascadeMarkAsDeleted(objToMarkInDB); //cascade deletion through the child objects of the child
				int nUpdatedRows = childObjectClassDao.update(objToMarkInDB);
				nrAffected += nUpdatedRows + nUpdateChildren;
			}
			
			System.out.println("finished cascadeMarkAsDeleted: " + parentObject + "\n   nrAffected="+nrAffected);
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return nrAffected;
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
	
	
	
	
	public ErrorResult errorResultFromException(Exception e, BeeCommandType commandType){
		String exceptionClassName = e.getClass().getName();
		String exceptionMessage = e.getMessage();
		return new ErrorResult(commandType, exceptionClassName, exceptionMessage);
	}
	
	@SuppressWarnings("unchecked")
	public BeeCommandResult select(SelectCommand command) {
		BeeCommandResult result = null;
		System.out.println("selected from DB:");
		
		try {
			Class objectClass = getObjectClass(command.getClassName());
			System.out.println("objects to be selected: " + objectClass);
			// instantiate the dao
			Dao<? super BeeObjectInterface, Integer> objectClassDao = DaoManager.createDao(ConnectionProvider.getConnectionSource(), objectClass);
			
			//select the objects from DB
			List<BeeObjectInterface> selectedObjects = null;
			switch(command.getSelectType()){
				case ALL:
					selectedObjects = (List<BeeObjectInterface>) objectClassDao.queryForAll();
					break;
				case WHERE:
					selectedObjects = queryWithWhere(objectClassDao, objectClass, command.getWhereString());
			}		
			
			if(selectedObjects !=null && selectedObjects.size()>0){
				//object were successfuly selected
				//convert list to json
				String jsonList = gson.toJson(selectedObjects, new TypeToken<List<BeeObjectInterface>>(){}.getType());
				result = new SelectCommandResult(jsonList);
			} else {
				result = new EmptyResult(command.getCommandType(), command.getClassName());
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
	
	private List<BeeObjectInterface> queryWithWhere(Dao<? super BeeObjectInterface, Integer> objectClassDao, Class objectClass, String whereStatement) throws SQLException, InstantiationException, IllegalAccessException {
		  
		 BeeObjectInterface obj = (BeeObjectInterface) objectClass.newInstance();
		 String queryString = "SELECT * FROM " + obj.getDBTableName() + " WHERE " + whereStatement;
		 
		 GenericRawResults<? super BeeObjectInterface> selectedResult = objectClassDao.queryRaw(queryString, objectClassDao.getRawRowMapper());
		 List<BeeObjectInterface> selectedList = (List<BeeObjectInterface>) selectedResult.getResults();
		 
		 return selectedList;
	}
	

}
