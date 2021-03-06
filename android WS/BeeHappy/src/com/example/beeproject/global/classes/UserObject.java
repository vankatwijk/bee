package com.example.beeproject.global.classes;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


/**
 * Class representing user object.
 * <p>Objects of this class can be persisted to a database using ORMLite
 * <p>THIS VERSION IS ONLY FOR CLIENT SIDE. Server must have its own different implementation
 *
 */
@DatabaseTable(tableName = "users")
public class UserObject implements BeeObjectInterface{
	
	@DatabaseField(generatedId = true)
    private int id;
    
    @DatabaseField(canBeNull = false)
    private String username;

    @DatabaseField(canBeNull = false)
    private String password;

    @DatabaseField(canBeNull = false)
	private boolean synced;
    
    @DatabaseField(canBeNull = true)
    private int serverSideID;
    
    /**
	 * Default no-arg constructor
	 * required by ORMLite
	 */
	public UserObject(){
	}
	
	public UserObject(int id) {
		this.id = id;
    }
	
	public UserObject(String username, String password){
		this.username = username;
		this.password = password;
	}

	public UserObject(int id, String username, String password){
		this.id = id;
		this.username = username;
		this.password = password;
	}
	
	public UserObject(int id, String username, String password, boolean synced, int serverSideID){
		this.id = id;
		this.username = username;
		this.password = password;
		this.synced = synced;
		this.serverSideID = serverSideID;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String getUsername(){
		return username;
	}
	
	public void setUsername(String username){
		this.username = username;
	}
	
	public String getPassword(){
		return password;
	}
	
	public void setPassword(String password){
		this.password = password;
	}

	@Override
	public String getDBTableName() {
		return "users";
	}
	
	@Override
	public String toString() {
		return "UserObject [id=" + id + ", username=" + username
				+ ", password=" + password + ", synced=" + synced + ", serverSideID="+serverSideID+" ]";
	}

	@Override
	public boolean isSynced() {
		return synced;
	}
	
	@Override
	public void setSynced(boolean synced) {
		this.synced = synced;
	}

	public int getServerSideID() {
		return serverSideID;
	}

	public void setServerSideID(int serverSideID) {
		this.serverSideID = serverSideID;
	}

	@Override
	public int refresh(DatabaseHelper db) {
		// no foreign objects, nothing to refresh
		return 0;
	}

	@Override
	public BeeObjectInterface getServerSideObject(DatabaseHelper db) {
		UserObject serverSideObject = new UserObject(id, username, password, synced, serverSideID);
		serverSideObject.setId(serverSideID); 
		serverSideObject.setPassword("pas"); // we dont want to send password to server - security!
		return serverSideObject;
	}

	
}
