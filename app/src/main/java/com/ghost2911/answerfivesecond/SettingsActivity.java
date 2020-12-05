package com.ghost2911.answerfivesecond;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    Button btnBack;
    Button btnChooseQuestion;
    Button btnContacts;
    Button btnRule;
    Button btnNoAds;
    private BillingClient mBillingClient;
    private Map<String, SkuDetails> mSkuDetailsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        BillingSettings();

        btnBack = (Button) findViewById(R.id.btnBack);
        btnChooseQuestion = (Button) findViewById(R.id.btnChooseQuestion);
        btnContacts = (Button) findViewById(R.id.btnContacts);
        btnRule = (Button) findViewById(R.id.btnRule);
        btnNoAds = (Button) findViewById(R.id.btnNoAds);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnChooseQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getPurchase("choose_questions")) {
                    Intent myIntent = new Intent(getApplicationContext(), ChooseQuestionsActivity.class);
                    startActivity(myIntent);
                }
                else
                    launchBilling("choose_questions");
            }
        });

        btnNoAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchBilling("noads");
            }
        });

        if (getPurchase("noads")){
            btnNoAds.setVisibility(View.INVISIBLE);
        }

        btnRule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogCustom));
                builder.setTitle("Правила игры")
                        .setMessage("Игрок зачитывает вопрос (или прослушивает аудио), после чего необходимо нажать большую кнопку для старта таймера. \n\nПосле этого у есть 5 секунд для 3 того чтобы придумать 3 варианта ответа на вопрос.\n\n" +
                                "Назвал 3 - получил 1 балл. Если нет, то на этот же вопрос отвечает следующий игрок, но он не может повторяться. \n\nХоды передаются по кругу.")
                        .setCancelable(false)
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        btnContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogCustom));
                builder.setTitle("Контактные данные")
                        .setMessage("Рекомендации по улучшению приложения, встречающиеся ошибки, вылеты и прочие вопросы\n\nE-mail: daniil2911@gmail.com")
                        .setCancelable(false)
                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void BillingSettings() {
        PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && purchases != null) {
                    for (Purchase purchase : purchases) {
                        if (purchase.getSku().equals("choose_question"))
                            Message("Вы разблокировали ВЫБОР ВОПРОСОВ!");
                        savePurchase(purchase.getSku());
                    }
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Message("Покупка отменена");
                } else {
                   Message("Код ошибки: "+billingResult.getResponseCode());
                }
            }
        };

        mBillingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails();
                    List<Purchase> purchasesList = queryPurchases();

                    for (int i = 0; i < purchasesList.size(); i++) {
                        String purchaseId = purchasesList.get(i).getSku();
                        if(TextUtils.equals("noads", purchaseId)) {
                            btnNoAds.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
               Message("Нет соединения с сервером для покупок");
            }
        });
    }

    //ПОЛУЧЕНИЕ ИНФОРМАЦИИ О ПОКУПКАХ
    private void querySkuDetails() {
        SkuDetailsParams.Builder skuDetailsParamsBuilder = SkuDetailsParams.newBuilder();
        List<String> skuList = new ArrayList<>();
        skuList.add("noads");
        skuList.add("choose_questions");
        skuDetailsParamsBuilder.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        mBillingClient.querySkuDetailsAsync(skuDetailsParamsBuilder.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    assert list != null;
                    for (SkuDetails skuDetails : list) {
                        mSkuDetailsMap.put(skuDetails.getSku(), skuDetails);
                    }
                }
            }
        });
    }

    //ВЫЗОВ ОКНА ПОКУПКИ
    public void launchBilling(String skuId) {
        try {
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(Objects.requireNonNull(mSkuDetailsMap.get(skuId)))
                    .build();
            mBillingClient.launchBillingFlow(this, billingFlowParams);
        }
        catch(Exception ex)
        {
            Message("Произошла ошибка во время покупки");
        }

    }

    //ЗАПРОС НА ПОЛУЧЕНИЕ СПИСКА СДЕЛАННЫХ ПОКУПОК
    private List<Purchase> queryPurchases() {
        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        return purchasesResult.getPurchasesList();
    }

    public void savePurchase(String skuId) {
        SharedPreferences activityPreferences = getSharedPreferences("purchases",MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();
        editor.putBoolean(skuId, true);
        editor.apply();
    }

    public Boolean getPurchase(String skuId) {
        SharedPreferences activityPreferences = getSharedPreferences("purchases",MODE_PRIVATE);
        return activityPreferences.getBoolean(skuId,false);
    }

    void Message(String text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }


}
