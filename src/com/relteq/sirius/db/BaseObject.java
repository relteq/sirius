package com.relteq.sirius.db;

import java.util.Calendar;
import java.util.Date;

@SuppressWarnings("serial")
public abstract class BaseObject extends org.apache.torque.om.BaseObject {
	public void setCreated(Date date) {}
	public void setModified(Date date) {}
	public void setCreatedBy(Long user_id) {}
	public void setModifiedBy(Long user_id) {}
	public void setModstamp(Date v) {}

	public Date getCreated() { return null; }

	private final static Long default_user = Long.valueOf(0);

	private void create(Long user_id, Date date) {
		setCreated(date);
		setCreatedBy(user_id);
		modify(user_id, date);
	}

	private void modify(Long user_id, Date date) {
		setModified(date);
		setModifiedBy(user_id);
		setModstamp(date);
	}

	private void createNow() {
		create(default_user, Calendar.getInstance().getTime());
	}

	private void modifyNow() {
		modify(default_user, Calendar.getInstance().getTime());
	}

	public void setNew(boolean is_new) {
		boolean was_new = isNew();
		super.setNew(is_new);
		if (is_new && !was_new) createNow();
	}

	public void setModified(boolean is_modified) {
		boolean was_modified = isModified();
		super.setModified(is_modified);
		if (is_modified) {
			if (isNew()) {
				if (null == getCreated()) createNow();
			} else if (!was_modified) modifyNow();
		}
	}

}
