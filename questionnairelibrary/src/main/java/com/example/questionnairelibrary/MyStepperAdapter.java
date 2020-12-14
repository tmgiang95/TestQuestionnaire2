package com.example.questionnairelibrary;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter;
import com.stepstone.stepper.viewmodel.StepViewModel;

public class MyStepperAdapter extends AbstractFragmentStepAdapter {
    private int totalQuestion;
    public MyStepperAdapter(FragmentManager fm, Context context, int totalQuestion) {
        super(fm, context);
        this.totalQuestion = totalQuestion;
    }

    @Override
    public Step createStep(int position) {
        final StepFragment step = new StepFragment();
        Bundle b = new Bundle();
        b.putInt("CURRENT_STEP_POSITION_KEY", position);
        step.setArguments(b);
        return step;
    }

    @Override
    public int getCount() {
        return totalQuestion;
    }

    @NonNull
    @Override
    public StepViewModel getViewModel(@IntRange(from = 0) int position) {
        //Override this method to set Step title for the Tabs, not necessary for other stepper types
        return new StepViewModel.Builder(context)
                .setTitle("Question "+(position+1)) //can be a CharSequence instead
                .create();
    }
}
