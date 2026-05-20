package com.safarpay.ui.converter;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.safarpay.R;
import com.safarpay.util.CountryCurrencyHelper;
import java.util.List;

public class ConverterFragment extends Fragment {

    private ConverterViewModel viewModel;
    private Spinner spinnerFrom, spinnerTo;
    private EditText etAmount;
    private TextView tvResult, tvRateInfo;
    private android.widget.ImageButton btnSwap;
    private List<String> currencies;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_converter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ConverterViewModel.class);

        spinnerFrom = view.findViewById(R.id.spinnerFrom);
        spinnerTo   = view.findViewById(R.id.spinnerTo);
        etAmount    = view.findViewById(R.id.etAmount);
        tvResult    = view.findViewById(R.id.tvResult);
        tvRateInfo  = view.findViewById(R.id.tvRateInfo);
        btnSwap     = view.findViewById(R.id.btnSwap);

        currencies = CountryCurrencyHelper.getCurrencyCodes();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            R.layout.spinner_item, currencies);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Default: PKR → USD
        spinnerFrom.setSelection(currencies.indexOf("PKR"));
        spinnerTo.setSelection(currencies.indexOf("USD"));

        btnSwap.setOnClickListener(v -> {
            int fromPos = spinnerFrom.getSelectedItemPosition();
            int toPos   = spinnerTo.getSelectedItemPosition();
            spinnerFrom.setSelection(toPos);
            spinnerTo.setSelection(fromPos);
            triggerConversion();
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b2, int c) { triggerConversion(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { triggerConversion(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spinnerFrom.setOnItemSelectedListener(spinnerListener);
        spinnerTo.setOnItemSelectedListener(spinnerListener);

        viewModel.getResult().observe(getViewLifecycleOwner(), tvResult::setText);
        viewModel.getRateInfo().observe(getViewLifecycleOwner(), tvRateInfo::setText);
    }

    private void triggerConversion() {
        String from   = spinnerFrom.getSelectedItem().toString();
        String to     = spinnerTo.getSelectedItem().toString();
        String amount = etAmount.getText().toString();
        if (!amount.isEmpty()) viewModel.fetchAndConvert(from, to, amount);
    }
}
