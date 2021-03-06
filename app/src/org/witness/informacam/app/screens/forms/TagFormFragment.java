package org.witness.informacam.app.screens.forms;

import info.guardianproject.odkparser.FormWrapper.ODKFormListener;

import java.io.FileNotFoundException;

import org.witness.informacam.InformaCam;
import org.witness.informacam.app.EditorActivity;
import org.witness.informacam.app.R;
import org.witness.informacam.app.utils.Constants;
import org.witness.informacam.app.utils.Constants.App.Editor;
import org.witness.informacam.app.utils.Constants.App.Editor.Forms;
import org.witness.informacam.app.utils.Constants.EditorActivityListener;
import org.witness.informacam.models.forms.IForm;
import org.witness.informacam.models.media.IRegion;
import org.witness.informacam.storage.FormUtility;
import org.witness.informacam.utils.Constants.App.Storage.Type;
import org.witness.informacam.utils.Constants.Logger;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class TagFormFragment extends Fragment implements ODKFormListener
{
	View rootView;
	LinearLayout tagFormRoot;

	Activity a;
	IForm form = null;
	IForm formNote = null;
	FormUtility formUtility;

	Handler h;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater li, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(li, container, savedInstanceState);

		rootView = li.inflate(R.layout.fragment_forms_fullscreen, null);
		tagFormRoot = (LinearLayout) rootView.findViewById(R.id.details_form_list);

		return rootView;
	}

	@Override
	public void onAttach(Activity a)
	{
		super.onAttach(a);
		this.a = a;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		h = new Handler();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.a != null && this.a instanceof EditorActivity)
		{
			((EditorActivity) a).onFragmentResumed(this);
		}
	}
	
	@Override
	public void onDetach()
	{
		super.onDetach();
	}

	public boolean initTag(final IRegion region)
	{
		// Reset
		tagFormRoot.removeAllViews();
		this.form = null;
		this.formNote = null;
		
		if (!region.associatedForms.isEmpty())
		{
			for (IForm form : region.associatedForms)
			{
				if (form.namespace.equals(Forms.TagForm.TAG))
				{
					this.form = IForm.Activate(form, a, InformaCam.getInstance().ioService.getBytes(form.answerPath, Type.IOCIPHER));
				}
				else if (form.namespace.equals(Forms.FreeText.TAG))
				{
					this.formNote = IForm.Activate(form, a, InformaCam.getInstance().ioService.getBytes(form.answerPath, Type.IOCIPHER));
				}
			}
		}

		if (this.form == null || this.formNote == null)
		{
			for (IForm form : ((EditorActivity) a).availableForms)
			{
				if (this.form == null && form.namespace.equals(Forms.TagForm.TAG))
				{
					this.form = IForm.Activate(form, a);
					this.form.answerPath = new info.guardianproject.iocipher.File(((EditorActivityListener) a).media().rootFolder, "form_"
							+ System.currentTimeMillis()).getAbsolutePath();
					region.addForm(this.form);
				}
				else if (this.formNote == null && form.namespace.equals(Forms.FreeText.TAG))
				{
					this.formNote = IForm.Activate(form, a);
					this.formNote.answerPath = new info.guardianproject.iocipher.File(((EditorActivityListener) a).media().rootFolder, "form_t"
							+ System.currentTimeMillis()).getAbsolutePath();
					region.addForm(this.formNote);
				}
			}
		}

		h.post(new Runnable() {			
			@Override
			public void run()
			{
				int[] inputTemplate = new int[] { R.layout.forms_odk_input_template, R.id.odk_question, R.id.odk_hint, R.id.odk_answer };
				int[] selectOneTemplate = new int[] { R.layout.forms_odk_select_one_template, R.id.odk_question, R.id.odk_hint, R.id.odk_selection_holder };
				int[] selectMultipleTemplate = new int[] { R.layout.forms_odk_select_multiple_template, R.id.odk_question, R.id.odk_hint,
						R.id.odk_selection_holder };

				// UI for the note
				for (View v : TagFormFragment.this.formNote.buildUI(inputTemplate, selectOneTemplate, selectMultipleTemplate, null))
				{
					tagFormRoot.addView(v);
				}

				LayoutInflater inflater = LayoutInflater.from(a);
				View divider = inflater.inflate(R.layout.extras_divider_blue, tagFormRoot, false);
				tagFormRoot.addView(divider);
				
				// UI for tag form
				for (View v : TagFormFragment.this.form.buildUI(inputTemplate, selectOneTemplate, selectMultipleTemplate, null))
				{
					tagFormRoot.addView(v);
				}
			}
		});

		return true;
	}

	public void saveTagFormData(final IRegion region)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					formNote.answerAll();
					formNote.save(new info.guardianproject.iocipher.FileOutputStream(formNote.answerPath));
					form.answerAll();
					form.save(new info.guardianproject.iocipher.FileOutputStream(form.answerPath));
					((EditorActivityListener) a).media().save();
				}
				catch (FileNotFoundException e)
				{
					Logger.e("error opening file for answer path", e);
				}
			}
		}).start();

	}

	@Override
	public boolean saveForm()
	{
		return true;

	}
}
