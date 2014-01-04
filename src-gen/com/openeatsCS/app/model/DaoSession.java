package com.openeatsCS.app.model;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import com.openeatsCS.app.model.Barcode;
import com.openeatsCS.app.model.History;

import com.openeatsCS.app.model.BarcodeDao;
import com.openeatsCS.app.model.HistoryDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig barcodeDaoConfig;
    private final DaoConfig historyDaoConfig;

    private final BarcodeDao barcodeDao;
    private final HistoryDao historyDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        barcodeDaoConfig = daoConfigMap.get(BarcodeDao.class).clone();
        barcodeDaoConfig.initIdentityScope(type);

        historyDaoConfig = daoConfigMap.get(HistoryDao.class).clone();
        historyDaoConfig.initIdentityScope(type);

        barcodeDao = new BarcodeDao(barcodeDaoConfig, this);
        historyDao = new HistoryDao(historyDaoConfig, this);

        registerDao(Barcode.class, barcodeDao);
        registerDao(History.class, historyDao);
    }
    
    public void clear() {
        barcodeDaoConfig.getIdentityScope().clear();
        historyDaoConfig.getIdentityScope().clear();
    }

    public BarcodeDao getBarcodeDao() {
        return barcodeDao;
    }

    public HistoryDao getHistoryDao() {
        return historyDao;
    }

}
