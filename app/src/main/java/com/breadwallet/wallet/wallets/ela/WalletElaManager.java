package com.breadwallet.wallet.wallets.ela;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWalletManager;
import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.BtcBchTransactionDataStore;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.WalletManagerHelper;
import com.breadwallet.wallet.wallets.ela.data.ElaTransactionEntity;
import com.elastos.jni.Utility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.tools.util.BRConstants.ROUNDING_MODE;

public class WalletElaManager extends BRCoreWalletManager implements BaseWalletManager {

    private static final String TAG = WalletElaManager.class.getSimpleName();

    public static final String ONE_ELA_IN_SALA = "100000000"; // 1 ela in sala, 100 millions
    public static final String MAX_ELA = "10000"; //Max amount in ela
    public static final String ELA_SYMBOL = "ELA";
    private static final String SCHEME = "elastos";
    private static final String NAME = "Elastos";
    private static final String ELA_ADDRESS_PREFIX = "E";

    public static final BigDecimal ONE_ELA = new BigDecimal(ONE_ELA_IN_SALA);

    private static WalletElaManager mInstance;

    private final BigDecimal MAX_SALA = new BigDecimal(MAX_ELA).multiply(new BigDecimal(ONE_ELA_IN_SALA));

    private final BigDecimal MIN_SALA = new BigDecimal("100");

    private WalletSettingsConfiguration mSettingsConfig;
    private WalletUiConfiguration mUiConfig;

    private WalletManagerHelper mWalletManagerHelper;

    protected static String mAddress;

    protected static String mPrivateKey;

    private static Context mContext;

    public static WalletElaManager getInstance(Context context) {

        if (mInstance == null) {
            mInstance = new WalletElaManager();
            mContext = context;
        }

        return mInstance;
    }

    private WalletElaManager() {
        mUiConfig = new WalletUiConfiguration("#003d79", null,
                true, WalletManagerHelper.MAX_DECIMAL_PLACES_FOR_UI);

        mSettingsConfig = new WalletSettingsConfiguration();

        mWalletManagerHelper = new WalletManagerHelper();
    }


    @Override
    public int getForkId() {
        Log.i(TAG, "getForkId");
        return -1;
    }

    @Override
    public BREthereumAmount.Unit getUnit() {
        Log.i(TAG, "getUnit");
        throw new RuntimeException("stub");
    }

    public static String getPrivateKey() {
        if (mPrivateKey == null) {
            try {
//                mPrivateKey = "A4FFD2C6258FC4ACA3D3573D929058DE60C0F7E561978E72EC1B9C2F9749E734";
                byte[] phrase = BRKeyStore.getPhrase(mContext, 0);
                mPrivateKey = Utility.getInstance(mContext).getSinglePrivateKey(new String(phrase));
                Log.i("test", "test");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mPrivateKey;
    }

    @Override
    public String getAddress() {
        Log.i(TAG, "getAddress");
        try {
            byte[] phrase = BRKeyStore.getPhrase(mContext, 0);
            String word = new String(phrase);
            Log.i("test", "test");
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        if (mAddress == null) {
            try {
                byte[] phrase = BRKeyStore.getPhrase(mContext, 0);
                String publickey = Utility.getInstance(mContext).getSinglePublicKey(new String(phrase));
                mAddress = Utility.getInstance(mContext).getAddress(publickey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // TODO: Test of we can remove the caching in memory and always call core directly.
        return mAddress;
    }

    @Override
    public boolean isAddressValid(String address) {
        Log.i(TAG, "isAddressValid");
        return !Utils.isNullOrEmpty(address) && address.startsWith(ELA_ADDRESS_PREFIX);
    }

    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed) {
        Log.i(TAG, "signAndPublishTransaction");
        if(tx == null) return new byte[1];
        BRElaTransaction raw = tx.getElaTx();
        if(raw == null) return new byte[1];
        String mRwTxid = ElaDataSource.getInstance(mContext).sendElaRawTx(raw.getTx());
        if(mRwTxid == null) return new byte[1];
        TxManager.getInstance().updateTxList(mContext);
        return mRwTxid.getBytes();
    }

    public void updateTxHistory() {
        ElaDataSource.getInstance(mContext).getTransactions(getAddress());
        TxManager.getInstance().updateTxList(mContext);
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener list) {
        mWalletManagerHelper.addBalanceChangedListener(list);
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        Log.i(TAG, "onBalanceChanged");
        mWalletManagerHelper.onBalanceChanged(balance);
    }

    @Override
    public void addSyncListener(SyncListener listener) {
        Log.i(TAG, "addSyncListener");
    }

    @Override
    public void removeSyncListener(SyncListener listener) {
        Log.i(TAG, "removeSyncListener");
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified list) {
        Log.i(TAG, "addTxListModifiedListener");
        mWalletManagerHelper.addTxListModifiedListener(list);
    }

    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
        Log.i(TAG, "watchTransactionForHash");
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        Log.i(TAG, "getRelayCount");
        return 0;
    }

    private static double time = 0;

    @Override
    public double getSyncProgress(long startHeight) {
        Log.i(TAG, "getSyncProgress");
//        time += 0.1;
//        if(time >= 1.0) time = 1.0;
        return /*time*/1.0;
    }

    @Override
    public double getConnectStatus() {
        Log.i(TAG, "getConnectStatus");
        return 2;
    }

    @Override
    public void connect(Context app) {
        Log.i(TAG, "connect");
    }

    @Override
    public void disconnect(Context app) {
        Log.i(TAG, "disconnect");
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        Log.i(TAG, "useFixedNode");
        return false;
    }

    @Override
    public void rescan(Context app) {
        Log.i(TAG, "rescan");
    }

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        Log.i(TAG, "getTxs");
        return new CryptoTransaction[0];
    }

    private static final BigDecimal ELA_FEE = new BigDecimal(100).divide(ONE_ELA, 8, BRConstants.ROUNDING_MODE);

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        Log.i(TAG, "getTxFee");
        return ELA_FEE;
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        return ELA_FEE;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return ELA_FEE;
    }

    @Override
    public String getTxAddress(CryptoTransaction tx) {
        return mAddress;
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        return MAX_SALA;
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return MIN_SALA;
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
        Log.i(TAG, "getTransactionAmount");
        return new BigDecimal(getWallet().getTransactionAmount(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return new BigDecimal(BRCoreTransaction.getMinOutputAmount());
    }

    @Override
    public void updateFee(Context app) {

    }

    @Override
    public void refreshAddress(Context app) {

    }

    @Override
    public void refreshCachedBalance(final Context app) {
        Log.i(TAG, "refreshCachedBalance");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String balance = ElaDataSource.getInstance(mContext).getElaBalance(getAddress());
                if(balance == null) return;
                final BigDecimal tmp = new BigDecimal((balance == null || balance.equals("")) ? "0" : balance);
                BRSharedPrefs.putCachedBalance(app, getIso(), tmp.multiply(ONE_ELA));
            }
        }).start();
    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        List<ElaTransactionEntity> transactionEntities = ElaDataSource.getInstance(mContext).getAllTransactions();
        List<TxUiHolder> uiTxs = new ArrayList<>();
        try {
            for (ElaTransactionEntity entity : transactionEntities) {
                TxUiHolder txUiHolder = new TxUiHolder(null,
                        entity.isReceived,
                        entity.timeStamp,
                        entity.blockHeight,
                        entity.hash,
                        entity.txReversed,
                        ELA_FEE,
                        entity.toAddress,
                        entity.fromAddress,
                        new BigDecimal(String.valueOf(entity.balanceAfterTx))
                        , entity.txSize,
                        new BigDecimal(String.valueOf(entity.amount))
                        , entity.isValid);
                uiTxs.add(txUiHolder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        return true;
    }

    @Override
    public boolean addressIsUsed(String address) {
        return true;
    }

    @Override
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public String getSymbol(Context app) {
        return "ELA";
    }

    @Override
    public String getIso() {
        return "ELA";//图标的选取，数量，价格都是根据这个iso来获取的
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDenominator() {
        return String.valueOf(ONE_ELA_IN_SALA);
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        return new CryptoAddress(mAddress, null);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        Log.i(TAG, "createTransaction");
        BRElaTransaction brElaTransaction = ElaDataSource.getInstance(mContext).createElaTx(getAddress(), address, amount.longValue(), "");
        return new CryptoTransaction(brElaTransaction);
    }

    @Override
    public String decorateAddress(String addr) {
        return addr;
    }

    @Override
    public String undecorateAddress(String addr) {
        return addr;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return WalletManagerHelper.MAX_DECIMAL_PLACES;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        Log.i(TAG, "getCachedBalance");
        return BRSharedPrefs.getCachedBalance(app, getIso());
    }

    //TODO wait
    @Override
    public BigDecimal getTotalSent(Context app) {
        Log.i(TAG, "getTotalSent");
        return BigDecimal.ZERO;
    }

    @Override
    public void wipeData(Context app) {
        BtcBchTransactionDataStore.getInstance(app).deleteAllTransactions(app, getIso());
        MerkleBlockDataSource.getInstance(app).deleteAllBlocks(app, getIso());
        PeerDataSource.getInstance(app).deleteAllPeers(app, getIso());
        BRSharedPrefs.clearAllPrefs(app);
    }

    @Override
    public void syncStarted() {
        Log.i(TAG, "syncStarted");
        mWalletManagerHelper.onSyncStarted();
    }

    @Override
    public void syncStopped(String error) {
        Log.i(TAG, "syncStopped");
        mWalletManagerHelper.onSyncStopped(error);
    }

    @Override
    public boolean networkIsReachable() {
        Log.i(TAG, "networkIsReachable");
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(), balance);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return MAX_SALA;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return mUiConfig;
    }

    @Override
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return mSettingsConfig;
    }

    private BigDecimal getFiatForEla(Context app, BigDecimal elaAmount, String code) {//总资产
        //fiat rate for btc
        CurrencyEntity btcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ela
        CurrencyEntity elaBtcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), "BTC");
        if (btcRate == null) {
            return null;
        }
        if (elaBtcRate == null) {
            return null;
        }

        return elaAmount.multiply(new BigDecimal(elaBtcRate.rate)).multiply(new BigDecimal(btcRate.rate));
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {//行情价格
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if (fiatData == null) return null;
        return fiatData; //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        BigDecimal amount = BRSharedPrefs.getCachedBalance(app, getIso()).divide(ONE_ELA);
        return getFiatForEla(app, amount, BRSharedPrefs.getPreferredFiatIso(app));
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        if (ent != null) {
            //passed in a custom CurrencyEntity
            //get crypto amount
            BigDecimal cryptoAmount = amount.divide(ONE_ELA, 8, BRConstants.ROUNDING_MODE);
            //multiply by fiat rate
            return cryptoAmount.multiply(new BigDecimal(ent.rate));
        }
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(ONE_ELA, 8, BRConstants.ROUNDING_MODE);

        BigDecimal fiatData = getFiatForEla(app, cryptoAmount, iso);
        if (fiatData == null) return null;
        return fiatData;
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) {
            return amount;
        }
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), iso);
        if (ent == null) {
            return null;
        }
        double rate = ent.rate;
        //convert c to $.
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso());
        BigDecimal result = BigDecimal.ZERO;
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.divide(new BigDecimal(rate), 2, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.divide(new BigDecimal(rate), getMaxDecimalPlaces(app), ROUNDING_MODE);
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        return amount.divide(ONE_ELA, 8, ROUNDING_MODE);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        BigDecimal result = BigDecimal.ZERO;
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso());
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.multiply(new BigDecimal("100"));
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = amount.multiply(new BigDecimal("100000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.multiply(new BigDecimal("100000000"));
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), iso);
        if (ent == null) {
            return amount;
        }
        double rate = ent.rate;
        //convert c to $.
        return amount.divide(new BigDecimal(rate), 8, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
    }
}
