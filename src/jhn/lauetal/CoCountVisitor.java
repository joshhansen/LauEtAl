package jhn.lauetal;

import jhn.wp.exceptions.CountException;
import jhn.wp.visitors.mongo.BasicMongoVisitor;

import com.mongodb.DBCollection;

public class CoCountVisitor extends BasicMongoVisitor {
	private DBCollection counts;
	private DBCollection cocounts;
	
	public CoCountVisitor(String server, int port, String dbName) {
		super(server, port, dbName);
	}
	
	@Override
	public void beforeEverything() throws Exception {
		super.beforeEverything();
		counts = db.getCollection("counts");
		cocounts = db.getCollection("cocounts");
	}
	
	@Override
	public void visitLabel(String label) throws CountException {
		// TODO Auto-generated method stub
		super.visitLabel(label);
	}

	@Override
	public void visitWord(String word) {
		// TODO Auto-generated method stub
		super.visitWord(word);
	}

	@Override
	public void afterLabel() throws Exception {
		// TODO Auto-generated method stub
		super.afterLabel();
	}
	
}
