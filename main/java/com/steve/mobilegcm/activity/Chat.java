package com.steve.mobilegcm.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.steve.mobilegcm.R;
import com.steve.mobilegcm.model.Conversation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Class Chat is the Activity class that holds main chat screen. It shows
 * all the conversation messages between two users and also allows the user to
 * send and receive messages.
 */
public class Chat extends Custom {

	/** The Conversation list. */
	private ArrayList<Conversation> conversationList;

	/** The chat adapter. */
	private ChatAdapter adapter;

	/** The Editext to compose the message. */
	private EditText messageEditText;

	/** The user name of username. */
	private String username;

	/** The date of last message in conversation. */
	private Date lastMsgDate;

	/** Flag to hold if the activity is running or not. */
	private boolean isRunning;

	/** The handler. */
	private static Handler handler;

    public static ParseUser user;

    private ParseUser otherParseUser;

    private List<ParseObject> conversationsFound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
        user = ParseUser.getCurrentUser();
		conversationList = new ArrayList<>();
		ListView list = (ListView) findViewById(R.id.list);
		adapter = new ChatAdapter();
		list.setAdapter(adapter);
		list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		list.setStackFromBottom(true);
		messageEditText = (EditText) findViewById(R.id.txt);
		messageEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		setTouchNClick(R.id.btnSend);
		username = getIntent().getStringExtra("username");
		setTitle("Μιλάτε με τον/την "+username);
		handler = new Handler();
        getOtherParseUser();
	}

    private void getOtherParseUser()
    {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", username);
        query.findInBackground(new FindCallback<ParseUser>()
        {
            @Override
            public void done(List<ParseUser> list, ParseException e)
            {
                if ( e == null )
                {
                    if ( list.size() != 0 )
                    {
                        otherParseUser = list.get( 0 );
                        getConversation();
                    }
                    else
                    {
                        Toast.makeText( Chat.this, "Αποτυχία: η λίστα με χρήστες " + username + " είναι άδεια", Toast.LENGTH_LONG ).show();
                    }
                }
                else
                {
                    Toast.makeText( Chat.this, "Αποτυχία: " + e.getMessage(), Toast.LENGTH_LONG ).show();
                }
            }
        });
    }

    private void getConversation()
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversation");
        query.whereEqualTo("userA", ParseUser.getCurrentUser().getObjectId() );
        query.whereEqualTo("userB", otherParseUser.getObjectId() );
        ParseQuery<ParseObject> query2 = ParseQuery.getQuery("Conversation");
        query2.whereEqualTo("userB", ParseUser.getCurrentUser().getObjectId() );
        query2.whereEqualTo("userA", otherParseUser.getObjectId() );
        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(query);
        queries.add(query2);
        ParseQuery<ParseObject> query3 = ParseQuery.or(queries);
        query3.findInBackground(new FindCallback<ParseObject>()
        {
            @Override
            public void done(List<ParseObject> list, ParseException e)
            {
                if ( e == null )
                {
                    if ( list.size() == 0 )
                    {
                        ParseObject conversation = new ParseObject( "Conversation" );
                        conversation.put( "userA", ParseUser.getCurrentUser().getObjectId() );
                        conversation.put( "isAOnline", true );
                        conversation.put( "isBOnline", false );
                        conversation.put( "userB", otherParseUser.getObjectId() );
                        conversation.saveInBackground();
                    }
                    else
                    {
                        conversationsFound = list;
                        if ( (list.get(0).get("userA")).equals(ParseUser.getCurrentUser().getObjectId()) )
                        {
                            list.get(0).put( "isAOnline", true );
                            list.get(0).saveInBackground();
                        }
                        else if ( (list.get(0).get("userB")).equals(ParseUser.getCurrentUser().getObjectId()) )
                        {
                            list.get(0).put( "isBOnline", true );
                            list.get(0).saveInBackground();
                        }
                    }
                }
            }
        });
    }

    private void updateUserStatus(boolean online) {
        if ( conversationsFound.get(0).get("userA").equals(ParseUser.getCurrentUser().getObjectId()) )
        {
            conversationsFound.get(0).put( "isAOnline", online );
            conversationsFound.get(0).saveInBackground();
        }
        else if ( conversationsFound.get(0).get("userB").equals(ParseUser.getCurrentUser().getObjectId()) )
        {
            conversationsFound.get(0).put( "isBOnline", online );
            conversationsFound.get(0).saveInBackground();
        }
    }

	@Override
	protected void onResume() {
		super.onResume();
		isRunning = true;
        if ( conversationsFound != null )
        {
            updateUserStatus(true);
        }
        loadConversationList();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateUserStatus(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUserStatus(false);
    }

	@Override
	protected void onPause() {
		super.onPause();
        updateUserStatus(false);
		isRunning = false;
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		if (v.getId() == R.id.btnSend) {
			sendMessage();
		}
	}

	/**
	 * Call this method to Send message to opponent. It does nothing if the text
	 * is empty otherwise it creates a Parse object for Chat message and send it
	 * to Parse server.
	 */
	private void sendMessage() {
		if (messageEditText.length() == 0)
			return;
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(messageEditText.getWindowToken(), 0);
		final String s = messageEditText.getText().toString();
		final Conversation c = new Conversation(s, new Date(), user.getUsername());
		c.setStatus(Conversation.STATUS_SENDING);
		conversationList.add(c);
		adapter.notifyDataSetChanged();
		messageEditText.setText(null);
		ParseObject po = new ParseObject("Chat");
		po.put("sender", user.getUsername());
		po.put("receiver", username);
		// po.put("createdAt", "");
		po.put("message", s);
		po.saveEventually(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                if (e == null)
                    c.setStatus(Conversation.STATUS_SENT);
                else
                    c.setStatus(Conversation.STATUS_FAILED);
                adapter.notifyDataSetChanged();
            }

        });
        if ( ( conversationsFound.get(0).get("userA").equals(otherParseUser.getObjectId()) && !((boolean)conversationsFound.get(0).get("isAOnline")))
                || conversationsFound.get(0).get("userB").equals(otherParseUser.getObjectId()) && !((boolean)conversationsFound.get(0).get("isBOnline")))
        {
            ParseQuery pushQuery = ParseInstallation.getQuery();
            pushQuery.whereEqualTo("user", otherParseUser);
            ParsePush push = new ParsePush();
            push.setQuery(pushQuery);
            push.setMessage(s);
            push.sendInBackground();
        }
	}

	/**
	 * Load the conversation list from Parse server and save the date of last
	 * message that will be used to load only recent new messages
	 */
	private void loadConversationList() {
		ParseQuery<ParseObject> q = ParseQuery.getQuery("Chat");
		if (conversationList.size() == 0)
		{
			// load all messages...
			ArrayList<String> al = new ArrayList<String>();
			al.add(username);
			al.add(user.getUsername());
			q.whereContainedIn("sender", al);
			q.whereContainedIn("receiver", al);
		}
		else {
			// load only newly received message..
			if (lastMsgDate != null)
				q.whereGreaterThan("createdAt", lastMsgDate);
			q.whereEqualTo("sender", username);
			q.whereEqualTo("receiver", user.getUsername());
		}
		q.orderByDescending("createdAt");
		q.setLimit(30);
		q.findInBackground(new FindCallback<ParseObject>() {

			@Override
			public void done(List<ParseObject> li, ParseException e) {
				if (li != null && li.size() > 0) {
					for (int i = li.size() - 1; i >= 0; i--) {
						ParseObject po = li.get(i);
						Conversation c = new Conversation(po.getString("message"), po.getCreatedAt(), po.getString("sender"));
						conversationList.add(c);
						if (lastMsgDate == null || lastMsgDate.before(c.getDate()))
							lastMsgDate = c.getDate();
						adapter.notifyDataSetChanged();
					}
				}
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						if (isRunning)
							loadConversationList();
					}
				}, 1000);
			}

		});
	}

	/**
	 * The Class ChatAdapter is the adapter class for Chat ListView. This
	 * adapter shows the Sent or Receieved Chat message in each list item.
	 */
	private class ChatAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return conversationList.size();
		}

		@Override
		public Conversation getItem(int arg0) {
			return conversationList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int pos, View v, ViewGroup arg2) {
			Conversation c = getItem(pos);
			if (c.isSent())
				v = getLayoutInflater().inflate(R.layout.chat_item_sent, null);
			else
				v = getLayoutInflater().inflate(R.layout.chat_item_rcv, null);
			TextView lbl = (TextView) v.findViewById(R.id.lbl1);
			lbl.setText(DateUtils.getRelativeDateTimeString(Chat.this, c.getDate().getTime(), DateUtils.SECOND_IN_MILLIS,
					DateUtils.DAY_IN_MILLIS, 0));
			lbl = (TextView) v.findViewById(R.id.lbl2);
			lbl.setText(c.getMsg());
			lbl = (TextView) v.findViewById(R.id.lbl3);
			if (c.isSent()) {
				if (c.getStatus() == Conversation.STATUS_SENT)
					lbl.setText("Delivered");
				else if (c.getStatus() == Conversation.STATUS_SENDING)
					lbl.setText("Sending...");
				else
					lbl.setText("Failed");
			}
			else
				lbl.setText("");

			return v;
		}

	}

}
