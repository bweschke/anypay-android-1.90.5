package com.anywherecommerce.anypaysampleappcloud;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.anywherecommerce.android.sdk.AnyPay;
import com.anywherecommerce.android.sdk.AuthenticationListener;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessage;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessageQueue;
import com.anywherecommerce.android.sdk.CommonErrors;
import com.anywherecommerce.android.sdk.GenericEventListener;
import com.anywherecommerce.android.sdk.GenericEventListenerWithParam;
import com.anywherecommerce.android.sdk.Logger;
import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.MeaningfulErrorListener;
import com.anywherecommerce.android.sdk.MeaningfulMessage;
import com.anywherecommerce.android.sdk.RequestListener;
import com.anywherecommerce.android.sdk.TaskListener;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.MultipleBluetoothDevicesFoundListener;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction;
import com.anywherecommerce.android.sdk.endpoints.anywherecommerce.CloudAPI;
import com.anywherecommerce.android.sdk.endpoints.worldnet.WorldnetEndpoint;
import com.anywherecommerce.android.sdk.models.CloudPosTerminalConnectionStatus;
import com.anywherecommerce.android.sdk.models.TransactionStatus;
import com.anywherecommerce.android.sdk.models.TransactionType;
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener;
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    protected Button initializeTerminalBtn, getCloudMsgBtn, audioConnectBtn, btConnectBtn, processTransacationBtn, logoutBtn;
    protected EditText activationCode, activationKey;
    protected LinearLayout loginLayout;
    protected TextView txtPanel;
    protected CloudPosTerminalMessage message;
    protected AnyPayTransaction transaction;
    protected CardReaderController cardReaderController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtPanel = findViewById(R.id.txtTextHarnessPanel);
        txtPanel.setMovementMethod(new ScrollingMovementMethod());

        initializeTerminalBtn = findViewById(R.id.initializeTerminal);
        getCloudMsgBtn = findViewById(R.id.getCloudMsg);
        btConnectBtn = findViewById(R.id.btConnect);
        audioConnectBtn = findViewById(R.id.audioConnect);
        processTransacationBtn = findViewById(R.id.processTransaction);
        activationCode = findViewById(R.id.activationCode);
        activationKey = findViewById(R.id.activationKey);
        loginLayout = findViewById(R.id.activationLayout);
        logoutBtn = findViewById(R.id.logout);

        changeState(processTransacationBtn, false);
        changeState(getCloudMsgBtn, false);

        if (!PermissionsController.verifyAppPermissions(this)) {
            PermissionsController.requestAppPermissions(this, PermissionsController.permissions, 1001);
        }

        try {
            Terminal.restoreState();

            changeState(getCloudMsgBtn, true);
            loginLayout.setVisibility(View.GONE);
            addText("Cloud Terminal Initialized. Endpoint details " + Terminal.getInstance().getEndpoint().getProvider());

        } catch (Exception ex) {
            addText("Terminal not initialized. Please initialize using the Initialize Cloud Terminal option");
        }

        AnyPay.getSupportKey("MY_PASSPHRASE", new RequestListener<String>() {
            @Override
            public void onRequestComplete(String s) {
                addText("Support Key - " + s);
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {

            }
        });

        cardReaderController = CardReaderController.getControllerFor(BBPOSDevice.class);

        subscribeCardReaderCallbacks();

        initializeTerminalBtn.setOnClickListener(initializeTerminal);

        getCloudMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchMessages();
            }
        });

        btConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("\nConnecting to BT\r\n");
                cardReaderController.connectBluetooth(new MultipleBluetoothDevicesFoundListener() {
                    @Override
                    public void onMultipleBluetoothDevicesFound(List<BluetoothDevice> matchingDevices) {
                        addText("Many BT devices");
                    }
                });
            }
        });

        audioConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("\nConnecting to audio jack (with polling)\r\n");
                cardReaderController.connectAudioJack();
            }
        });


        processTransacationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processTransaction();
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtPanel.setText("");
                CloudPosTerminalMessageQueue.getInstance().unsubscribeAllToMessagesOfType("NEW_TRANSACTION");
                CloudPosTerminalMessageQueue.getInstance().unsubscribeAllToMessagesOfType("CANCEL_TRANSACTION");
                CloudPosTerminalMessageQueue.getInstance().unsubscribeAllToMessagesOfType("CONFIG_CHANGED");
                CloudPosTerminalMessageQueue.getInstance().stop();
                Terminal.clearSavedState();
                loginLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void subscribeCardReaderCallbacks() {

        cardReaderController.subscribeOnCardReaderConnected(new GenericEventListenerWithParam<CardReader>() {
            @Override
            public void onEvent(CardReader deviceInfo) {
                if (deviceInfo == null)
                    addText("\r\nUnknown device connected");
                else
                    addText("\nDevice connected " + deviceInfo.getModelDisplayName());
            }
        });

        cardReaderController.subscribeOnCardReaderDisconnected(new GenericEventListener() {
            @Override
            public void onEvent() {
                addText("\nDevice disconnected");
            }
        });

        cardReaderController.subscribeOnCardReaderConnectFailed(new MeaningfulErrorListener() {
            @Override
            public void onError(MeaningfulError error) {
                addText("\nDevice connect failed: " + error.toString());
            }
        });

        cardReaderController.subscribeOnCardReaderError(new MeaningfulErrorListener() {
            @Override
            public void onError(MeaningfulError error) {
                addText("\nDevice error: " + error.toString());
            }
        });
    }

    View.OnClickListener initializeTerminal = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            initializeTerminalBtn.setText("Initializing...");
            initializeTerminalBtn.setEnabled(false);

            if (TextUtils.isEmpty(activationCode.getText().toString()) || TextUtils.isEmpty(activationKey.getText().toString())) {
                addText("Please enter activation code and key in the text fields");
                return;
            }

            Terminal.initializeFromCloud(activationCode.getText().toString(), activationKey.getText().toString(), new TaskListener() {
                @Override
                public void onTaskComplete() {
                    initializeTerminalBtn.setText("Initialize Cloud Terminal");
                    initializeTerminalBtn.setEnabled(true);

                    addText("Cloud Terminal Initialized. Endpoint details " + Terminal.getInstance().getEndpoint().getProvider());
                    changeState(getCloudMsgBtn, true);

                    loginLayout.setVisibility(View.GONE);
                }

                @Override
                public void onTaskFailed(MeaningfulError meaningfulError) {
                    initializeTerminalBtn.setText("Initialize Cloud Terminal");
                    initializeTerminalBtn.setEnabled(true);

                    Toast.makeText(MainActivity.this, "Terminal Initialization failed " + meaningfulError.message, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private void fetchMessages() {

        getCloudMsgBtn.setEnabled(false);

        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("NEW_TRANSACTION", new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
            @Override
            public void onEvent(CloudPosTerminalMessage message) {
                addText("Transaction available to process. Please accept the transaction and process it");
                transaction = message.transaction;

                message.accept();

                acceptTransaction();
            }
        });

        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("CANCEL_TRANSACTION", new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
            @Override
            public void onEvent(CloudPosTerminalMessage message) {
                addText("Cancel Transaction Message received");

                if (transaction == null) {
                    //addText("Request Rejected");
                    addText("Transaction is null - but we're still receiving a request to cancel it. We're going to accept anyway.");
                    //message.reason = "No transaction in progress";
                    //message.reject();
                    addText("Transaction UUID is: "+message.transactionUUID);
                    AnyPayTransaction tr = new AnyPayTransaction();
                    tr.setUuid(message.transactionUUID);
                    tr.setStatus(TransactionStatus.CANCELLED);
                    tr.setId("555343");
                    message.accept();
                    Terminal.getInstance().updateCloudTransaction(tr, new RequestListener<Void>() {
                        @Override
                        public void onRequestComplete(Void aVoid) {
                            addText("Transaction Status Updated");
                        }

                        @Override
                        public void onRequestFailed(MeaningfulError meaningfulError) {
                            addText("Transaction Update Failed " + meaningfulError.message);
                        }
                    });
                }
                else if ((boolean)(transaction.getCustomField("ReaderProcessingStarted", false))) {
                    message.reason = "Transaction cannot be cancelled at this stage";
                    message.reject();
                }
                else if (transaction.getUuid().equalsIgnoreCase(message.transactionUUID))  // Same transaction as the one in progress.
                {
                    try {
                        transaction.cancel();
                        message.accept();

                        transaction.setStatus(TransactionStatus.CANCELLED);

                    } catch (Exception ex) {
                        message.fail(new MeaningfulError(ex));
                    }
                } else {
                    message.reason = "Wrong transaction";
                    message.reject();
                }
            }
        });

        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("CONFIG_CHANGED", new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
            @Override
            public void onEvent(CloudPosTerminalMessage message) {
                Terminal.getInstance().overwriteConfiguration(message.terminal);
                message.accept();

                addText("Terminal Configuration Updated");
            }
        });


        CloudPosTerminalMessageQueue.getInstance().OnConnectionStatusChanged = new GenericEventListenerWithParam<CloudPosTerminalConnectionStatus>() {
            @Override
            public void onEvent(CloudPosTerminalConnectionStatus cloudPosTerminalConnectionStatus) {
                if (MainActivity.this.transaction == null) {
                    switch (cloudPosTerminalConnectionStatus) {
                        case CONNECTED:
                            addText("Waiting for Transaction...");
                            break;

                        case CONNECTING:
                        case RECONNECTING:
                            addText("Terminal Connecting...");
                            break;

                        case DISCONNECTED:
                        case DISCONNECTING:
                            addText("Terminal Disconnected. Check your network");
                            break;
                    }
                }
            }
        };

        CloudPosTerminalMessageQueue.getInstance().start();
    }

    private void acceptTransaction() {
        addText("Accepting Transaction. Please Wait...");

        this.transaction.setStatus(TransactionStatus.PROCESSING);

        CloudAPI.acceptTransaction(Terminal.getInstance(), this.transaction, new RequestListener<Void>() {
            @Override
            public void onRequestComplete(Void aVoid) {
                addText("Transaction Accepted. Please process the transaction");
                //changeState(processTransacationBtn, true);
                processTransaction();
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                addText("Some error occurred");
            }
        });
    }

    private void processTransaction() {
        if (!CardReaderController.isCardReaderConnected()) {
            addText("Transaction Failed. Card Reader not connected. Updating status on cloud...");

            this.transaction.setStatus(TransactionStatus.FAILED);
            updateTransaction(this.transaction);

            this.transaction = null;

            changeState(processTransacationBtn, false);

            return;
        }

        if (transaction.getTransactionType() != TransactionType.SALE) {
            this.transaction.execute(new TransactionListener() {
                @Override
                public void onTransactionCompleted() {
                    updateTransaction(transaction);

                    if (transaction.isApproved()) {
                        addText("Transaction Approved");
                    }
                    else {
                        addText("Transaction Declined - " + transaction.getResponseText());
                    }

                    changeState(processTransacationBtn, false);

                    transaction = null;

                }

                @Override
                public void onTransactionFailed(MeaningfulError meaningfulError) {
                    addText("Transaction Failed " + meaningfulError.message);

                    changeState(processTransacationBtn, false);

                    updateTransaction(transaction);
                    transaction = null;
                }
            });
        }
        else {
            this.transaction.useCardReader(CardReaderController.getConnectedReader());

            this.transaction.execute(new CardTransactionListener() {
                @Override
                public void onCardReaderEvent(MeaningfulMessage meaningfulMessage) {
                    addText(meaningfulMessage.message);
                }

                @Override
                public void onTransactionCompleted() {
                    updateTransaction(transaction);

                    if (transaction.isApproved()) {
                        addText("Transaction Approved");
                    }
                    else {
                        addText("Transaction Declined - " + transaction.getResponseText());
                    }

                    changeState(processTransacationBtn, false);

                    transaction = null;

                }

                @Override
                public void onTransactionFailed(MeaningfulError meaningfulError) {
                    addText("Transaction Failed " + meaningfulError.message);

                    changeState(processTransacationBtn, false);

                    updateTransaction(transaction);
                    transaction = null;
                }
            });
        }

    }

    private void updateTransaction(AnyPayTransaction tr) {
        Terminal.getInstance().updateCloudTransaction(tr, new RequestListener<Void>() {
            @Override
            public void onRequestComplete(Void aVoid) {
                addText("Transaction Status Updated");
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                addText("Transaction Update Failed " + meaningfulError.message);
            }
        });
    }

    private void addText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtPanel.append("\r\n" + text + "\r\n\n");
            }
        });
    }

    private void changeState(Button btn, boolean enabled) {
        btn.setEnabled(enabled);
        btn.setAlpha(enabled ? 1f : 0.5f);
    }
}
