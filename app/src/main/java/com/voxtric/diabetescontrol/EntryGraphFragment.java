package com.voxtric.diabetescontrol;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

public class EntryGraphFragment extends Fragment
{
  public EntryGraphFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    ConstraintLayout view = (ConstraintLayout)inflater.inflate(R.layout.fragment_entry_graph, container, false);
    return view;
  }
}
