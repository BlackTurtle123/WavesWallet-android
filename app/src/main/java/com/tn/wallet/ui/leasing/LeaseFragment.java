package com.tn.wallet.ui.leasing;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.tn.wallet.R;
import com.tn.wallet.api.NodeManager;
import com.tn.wallet.data.connectivity.ConnectivityStatus;
import com.tn.wallet.data.datamanagers.AddressBookManager;
import com.tn.wallet.databinding.FragmentLeaseBinding;
//import com.tn.wallet.databinding.FragmentSendBinding;
import com.tn.wallet.databinding.FragmentLeaseConfirmBinding;
import com.tn.wallet.databinding.FragmentLeaseSuccessBinding;
import com.tn.wallet.databinding.FragmentSendConfirmBinding;
import com.tn.wallet.databinding.FragmentSendSuccessBinding;
import com.tn.wallet.payload.LeaseTransaction;
import com.tn.wallet.payload.Transaction;
import com.tn.wallet.request.LeaseTransactionRequest;
import com.tn.wallet.request.TransferTransactionRequest;
import com.tn.wallet.ui.assets.ItemAccount;
import com.tn.wallet.ui.assets.LeaseConfirmationDetails;
import com.tn.wallet.ui.assets.PaymentConfirmationDetails;
import com.tn.wallet.ui.auth.PinEntryActivity;
import com.tn.wallet.ui.balance.TransactionsFragment;
import com.tn.wallet.ui.customviews.ReselectSpinner;
import com.tn.wallet.ui.customviews.ToastCustom;
import com.tn.wallet.ui.send.AddressBookAdapter;
import com.tn.wallet.ui.send.AddressItem;
import com.tn.wallet.ui.send.AssetAccountAdapter;
import com.tn.wallet.ui.send.FeeAdapter;
import com.tn.wallet.ui.send.FeeItem;
//import com.tn.wallet.ui.send.SendViewModel;
//import com.tn.wallet.ui.zxing.CaptureActivity;
import com.tn.wallet.ui.transactions.LeaseDetailActivity;
import com.tn.wallet.ui.zxing.CaptureActivity;
import com.tn.wallet.util.AndroidUtils;
import com.tn.wallet.util.AppRate;
import com.tn.wallet.util.AppUtil;
import com.tn.wallet.util.DateUtil;
import com.tn.wallet.util.MoneyUtil;
import com.tn.wallet.util.PermissionUtil;
import com.tn.wallet.util.ViewUtils;
import com.tn.wallet.util.annotations.Thunk;

import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.databinding.DataBindingUtil.inflate;
import static com.tn.wallet.ui.auth.PinEntryFragment.KEY_VALIDATED_PASSWORD;
import static com.tn.wallet.ui.auth.PinEntryFragment.KEY_VALIDATING_PIN_FOR_RESULT;
import static com.tn.wallet.ui.auth.PinEntryFragment.REQUEST_CODE_VALIDATE_PIN;
import static com.tn.wallet.ui.balance.TransactionsFragment.KEY_TRANSACTION_LIST_POSITION;
import static com.tn.wallet.ui.balance.TransactionsFragment.TX_AS_JSON;


public class LeaseFragment extends Fragment implements LeaseViewModel.DataListener {

//    private static final String ARG_INCOMING_URI = "incoming_uri";
//    private static final String ARG_SELECTED_POSTION = "selected_position";
    private static final int SCAN_URI = 2007;
    private static final int SCAN_PRIVX = 2008;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    @Thunk FragmentLeaseBinding binding;
    @Thunk LeaseViewModel viewModel;
    private OnLeaseFragmentInteractionListener listener;
    private TextWatcher amountTextWatcher;
    private TextWatcher feeTextWatcher;

//    private String incomingUri;
    private long backPressed;
    private int selectedAccountPosition = -1;

    private AlertDialog confirmDialog;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
/*            if (intent.getAction().equals(TransactionsFragment.ACTION_INTENT) && binding != null) {
                ((AssetAccountAdapter) binding.accounts.spinner.getAdapter()).updateData(viewModel.getAssetsList());
            }*/
        }
    };

    public LeaseFragment() {
        // Required empty public constructor
    }

    public static LeaseFragment newInstance(/*String incomingUri,*/ int selPos) {
        LeaseFragment fragment = new LeaseFragment();
/*        Bundle args = new Bundle();
        args.putString(ARG_INCOMING_URI, incomingUri);
        args.putInt(ARG_SELECTED_POSTION, selPos);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*if (getArguments() != null) {
            incomingUri = getArguments().getString(ARG_INCOMING_URI);
            selectedAccountPosition = getArguments().getInt(ARG_SELECTED_POSTION);
        }*/

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_lease, container, false);
        viewModel = new LeaseViewModel(getContext(), this);
        binding.setViewModel(viewModel);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setupToolbar();

        setupViews();

        viewModel.addActiveLeaseListener(new ActiveLeaseListener() {
            @Override
            public void activeLeasesReceived(LeaseTransaction[] leaseTransactions) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (LeaseTransaction tx: leaseTransactions) {
                            DateUtil mDateUtil = new DateUtil(getContext());
                            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.txs_layout, container, false);

                            ((TextView)view.findViewById(R.id.result)).setText("" + (tx.amount / Math.pow(10, 8)));
                            ((TextView)view.findViewById(R.id.counter_party)).setText(tx.recipient);
                            ((TextView)view.findViewById(R.id.units)).setText("TN");
                            ((TextView)view.findViewById(R.id.ts)).setText(mDateUtil.formatted(tx.timestamp));
                            binding.activeLeaseList.addView(view);
                            view.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    System.out.println("tx clicked");
                                    goToTransactionDetail(tx, 1);
                                }
                            });
                        }
                    }
                });
            }
        });

//        if (incomingUri != null) viewModel.handleIncomingUri(incomingUri);

        setHasOptionsMenu(true);

        return binding.getRoot();
    }

    void goToTransactionDetail(Transaction tx, int position) {
        if (tx instanceof LeaseTransaction) {
            Gson gson = new Gson();
            Intent intent = new Intent(getActivity(), LeaseDetailActivity.class);
            //intent.putExtra(KEY_TRANSACTION_LIST_POSITION, position);
            System.out.println (gson.toJson(tx));
            intent.putExtra(TX_AS_JSON, gson.toJson(tx));
            startActivity(intent);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(TransactionsFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

        binding.destination.setLines(2);
        binding.destination.setHorizontallyScrolling(false);

/*        if (listener != null) {
            listener.onSendFragmentStart();
        }*/
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    private void setupToolbar() {
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.leasing);

            ViewUtils.setElevation(
                    getActivity().findViewById(R.id.appbar_layout),
                    ViewUtils.convertDpToPixel(5F, getContext()));
        } else {
            finishPage();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.lease_activity_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /*@Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.action_qr_main);
        menuItem.setVisible(false);
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_qr:
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromFragment(binding.getRoot(), this);
                } else {
                    startScanActivity(SCAN_URI);
                }
                return true;
            case R.id.action_lease:
                //customKeypad.setNumpadVisibility(View.GONE);

                if (ConnectivityStatus.hasConnectivity(getActivity())) {
                    viewModel.sendClicked();
                } else {
                    ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanActivity(int code) {
        if (!new AppUtil(getActivity()).isCameraOpen()) {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            startActivityForResult(intent, code);
        } else {
            ToastCustom.makeText(getActivity(), getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {

            viewModel.handleIncomingUri(data.getStringExtra(CaptureActivity.SCAN_RESULT));

        } else if (requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK) {
            final String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);
        } else if (requestCode == REQUEST_CODE_VALIDATE_PIN && resultCode == RESULT_OK && data != null
            && data.getStringExtra(KEY_VALIDATED_PASSWORD) != null) {
            LeaseTransactionRequest singed = viewModel.signTransaction();
            if (singed != null) {
                viewModel.submitLease(singed);
            }
        }
    }

    public void onBackPressed() {
        handleBackPressed();
    }

    public void handleBackPressed() {
        if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            if (AndroidUtils.is16orHigher()) {
                getActivity().finishAffinity();
            } else {
                getActivity().finish();
                // Shouldn't call System.exit(0) if it can be avoided
                System.exit(0);
            }
            return;
        } else {
            onExitConfirmToast();
        }

        backPressed = System.currentTimeMillis();
    }

    public void onExitConfirmToast() {
        ToastCustom.makeText(getActivity(), getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity(SCAN_URI);
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setupViews() {

        setupDestinationView();
        setupSendFromView();

        setupAmountView();
        //setupFeeView();

        /*binding.max.setOnClickListener(view -> {
            viewModel.spendAllClicked();
        });*/
    }

    private void setupDestinationView() {
        binding.destination.setLines(2);
        binding.destination.setHorizontallyScrolling(false);

        setupEditableSpinner(binding.destination, binding.spDestination);

        binding.spDestination.setAdapter(new AddressBookAdapter(
                getContext(),
                AddressBookManager.get().getAllList()));

        binding.spDestination.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        AddressItem selectedItem = (AddressItem) binding.spDestination.getSelectedItem();
                        binding.destination.setText(selectedItem.address);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

    }

    private void setupEditableSpinner(EditText editText, ReselectSpinner spinner) {
        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            TextInputEditText anEditText = (TextInputEditText) v;
            if(event.getAction() == MotionEvent.ACTION_UP) {
                if(event.getRawX() >= (anEditText.getRight() - anEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    spinner.performClick();

                    return true;
                }
            }
            return false;
        });

        // Set drop down width equal to clickable view
        spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    spinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                /*if (binding.accounts.spinner.getWidth() > 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        spinner.setDropDownWidth(binding.accounts.spinner.getWidth());
                    }*/
            }
        });
    }

    private void setupSendFromView() {
        /*binding.accounts.spinner.setAdapter(new AssetAccountAdapter(
                getContext(),
                R.layout.spinner_item,
                viewModel.getAssetsList()));*/

        // Set drop down width equal to clickable view
        /*binding.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    binding.accounts.spinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.setDropDownWidth(binding.accounts.spinner.getWidth());
                }
            }
        });*/
        /*binding.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                viewModel.setSendingAssets(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });*/


        /*if (selectedAccountPosition > 0) {
            binding.accounts.spinner.setSelection(selectedAccountPosition);
        } else {
            binding.accounts.spinner.setSelection(0);
        }*/
    }

    @Override
    public void onHideSendingAddressField() {
        //binding.fromRow.setVisibility(View.GONE);
    }


    @Override
    public void onShowInvalidAmount() {
        onShowToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR);
    }


    @Override
    public void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        // TODO: 05/01/2017 Shouldn't have to explicitly run on UI thread, convert calling method to Rx and schedule appropriately
        getActivity().runOnUiThread(() -> ToastCustom.makeText(getActivity(), getString(message), ToastCustom.LENGTH_SHORT, toastType));
    }

    private void toggleFavorite(String address, FragmentLeaseSuccessBinding binding) {
        if (AddressBookManager.get().getAll().containsKey(address)) {
            binding.btnFavorite.setImageResource(R.drawable.ic_star);
            binding.btnFavorite.setOnClickListener(null);
        }
    }

    private AlertDialog createLeaseSuccessDialog(LeaseTransactionRequest signed) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        FragmentLeaseSuccessBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()),
                R.layout.fragment_lease_success, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        AlertDialog successDialog= dialogBuilder.create();
        successDialog.setCanceledOnTouchOutside(false);
        dialogBinding.btnDone.setOnClickListener(v -> successDialog.dismiss());
        dialogBinding.ivCheck.setOnClickListener(v -> successDialog.dismiss());

        /*final AddressBookManager.AddressBookListener listener = new AddressBookManager.AddressBookListener() {
            @Override
            public void onAddressAdded(String address, String name) {
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcastSync(new Intent(TransactionsFragment.ACTION_INTENT));
                toggleFavorite(address, dialogBinding);
            }

            @Override
            public void onAddressRemoved(String address) {}
        };

        dialogBinding.btnFavorite.setOnClickListener(v -> {
            AddressBookManager.createEnterNameDialog(getContext(), signed.recipient, listener).show();
        });*/

        toggleFavorite(signed.recipient, dialogBinding);

        return successDialog;
    }

    /*@Override
    public void onShowTransactionSuccess(TransferTransactionRequest signed) {
        getActivity().runOnUiThread(() -> {
            confirmDialog.cancel();
            playAudio();

            NodeManager.get().addPendingTransaction(signed.createDisplayTransaction());
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcastSync(new Intent(TransactionsFragment.ACTION_INTENT));

            final AlertDialog successDialog = createSendSuccessDialog(signed);

            AppRate appRate = new AppRate(getActivity())
                    .setMinTransactionsUntilPrompt(3)
                    .incrementTransactionCount();

            // If should show app rate, success dialog shows first and launches
            // rate dialog on dismiss. Dismissing rate dialog then closes the page. This will
            // happen if the user chooses to rate the app - they'll return to the main page.
            if (appRate.shouldShowDialog()) {
                AlertDialog ratingDialog = appRate.getRateDialog();
                ratingDialog.setOnDismissListener(d -> finishPage());
                successDialog.show();
                successDialog.setOnDismissListener(d -> ratingDialog.show());
            } else {
                successDialog.show();
                successDialog.setOnDismissListener(dialogInterface -> finishPage());
            }

        });
    }*/

    @Override
    public void onShowLeaseSuccess(LeaseTransactionRequest signed) {
        getActivity().runOnUiThread(() -> {
            playAudio();

            NodeManager.get().addPendingTransaction(signed.createDisplayTransaction());
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcastSync(new Intent(TransactionsFragment.ACTION_INTENT));

            final AlertDialog successDialog = createLeaseSuccessDialog(signed);

            AppRate appRate = new AppRate(getActivity())
                    .setMinTransactionsUntilPrompt(3)
                    .incrementTransactionCount();

            // If should show app rate, success dialog shows first and launches
            // rate dialog on dismiss. Dismissing rate dialog then closes the page. This will
            // happen if the user chooses to rate the app - they'll return to the main page.
            if (appRate.shouldShowDialog()) {
                AlertDialog ratingDialog = appRate.getRateDialog();
                ratingDialog.setOnDismissListener(d -> finishPage());
                successDialog.show();
                successDialog.setOnDismissListener(d -> ratingDialog.show());
            } else {
                successDialog.show();
                successDialog.setOnDismissListener(dialogInterface -> finishPage());
            }

        });
    }

    private void setupAmountView() {
        amountTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable editable) {
                binding.amount.removeTextChangedListener(amountTextWatcher);
                viewModel.leaseModel.setAmount(MoneyUtil.convertToCorrectFormat(editable.toString(), viewModel.leaseModel.sendingAsset));
                if (viewModel.leaseModel.getAmount().isEmpty()) {
                    onShowInvalidAmount();
                }
                binding.amount.addTextChangedListener(amountTextWatcher);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };

        //setDecimalKeyListener(binding.amount);
        binding.amount.addTextChangedListener(amountTextWatcher);
        binding.amount.setSelectAllOnFocus(true);
        binding.amount.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.amount.postDelayed(() -> binding.amount.setHint("0.00"), 200);
            } else {
                binding.amount.setHint("");
            }
        });

        //binding.amount.setHint("0" + viewModel.getDefaultSeparator() + "00");

    }

    /*private void setDecimalKeyListener(EditText et) {
        binding.customFee.setKeyListener(DigitsKeyListener.getInstance("0123456789" + MoneyUtil.getDefaultDecimalSeparator()));
    }

    private void setupFeeView() {
        feeTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable editable) {
                /binding.customFee.removeTextChangedListener(feeTextWatcher);
                viewModel.leaseModelsetCustomFee(MoneyUtil.convertToCorrectFormat(editable.toString(), viewModel.leaseModelfeeAsset));
                if (viewModel.leaseModelgetCustomFee().isEmpty()) {
                    onShowInvalidAmount();
                }
                viewModel.updateMaxAvailable();
                binding.customFee.addTextChangedListener(feeTextWatcher);
                //setKeyListener(editable, binding.amount);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        binding.customFee.addTextChangedListener(feeTextWatcher);
        setDecimalKeyListener(binding.customFee);

        setupEditableSpinner(binding.customFee, binding.spFee);

        binding.spFee.setAdapter(new FeeAdapter(getContext()));

        binding.spFee.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        FeeItem selectedItem = (FeeItem) binding.spFee.getSelectedItem();
                        viewModel.leaseModelsetCustomFee(selectedItem.fee);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        binding.customFee.setSelectAllOnFocus(true);
        //binding.feeRow.setHintAnimationEnabled(false);
        //binding.feeRow.setHintAnimationEnabled(true);
    }*/

    @Override
    public void onShowLeaseDetails(LeaseConfirmationDetails details) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        FragmentLeaseConfirmBinding dialogBinding = inflate(LayoutInflater.from(getActivity()),
                R.layout.fragment_lease_confirm, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        confirmDialog= dialogBuilder.create();
        confirmDialog.setCanceledOnTouchOutside(true);
        confirmDialog.setCancelable(true);

        dialogBinding.confirmFromLabel.setText(details.fromLabel);
        dialogBinding.confirmToLabel.setText(details.toLabel);
        dialogBinding.confirmAmount.setText(details.amount);
        dialogBinding.confirmFee.setText(details.fee);

        dialogBinding.confirmLeaseCancel.setOnClickListener(v -> {
            if (confirmDialog.isShowing()) {
                confirmDialog.cancel();
            }
        });

        dialogBinding.confirmLeaseButton.setOnClickListener(v -> {
            confirmDialog.dismiss();
            confirmDialog.cancel();

            if (ConnectivityStatus.hasConnectivity(getActivity())) {
                confirmSend(dialogBinding);
            } else {
                ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                // Queue tx here
            }
        });

        if (getActivity() != null && !getActivity().isFinishing()) {
            confirmDialog.show();
        }

        // To prevent the dialog from appearing too large on Android N
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

    }

    private void confirmSend(FragmentLeaseConfirmBinding dialogBinding) {
        //dialogBinding.confirmLeaseButton.setClickable(false);

        LeaseTransactionRequest signed = viewModel.signTransaction();
        if (signed != null) {
            viewModel.submitLease(signed);
        } else {
            requestPinDialog();
        }
    }

    private void requestPinDialog() {
        Intent intent = new Intent(getActivity(), PinEntryActivity.class);
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true);
        startActivityForResult(intent, REQUEST_CODE_VALIDATE_PIN);
    }

    private void playAudio() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.beep);
            mp.setOnCompletionListener(mp1 -> {
                mp1.reset();
                mp1.release();
            });
            mp.start();
        }
    }

    @Override
    public void finishPage() {
        if (listener != null) {
            listener.onLeaseFragmentClose();
        }
    }

    @Override
    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onSetSelection(int pos) {
        //binding.accounts.spinner.setSelection(pos);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLeaseFragmentInteractionListener) {
            listener = (OnLeaseFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnLeaseFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnLeaseFragmentInteractionListener {

        void onLeaseFragmentClose();

        void onLeaseFragmentStart();
    }
}
