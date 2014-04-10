package ro.pub.cs.pdsd.lab8;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	private String greeting = "Hello world";
	private SingleThreadedServer greetingServer = null;
	private Thread greetingThread; 
	private final static String TAG = "MainActivity";


	private class SingleThreadedServer implements Runnable {

		private final static String TAG = "ServerThread";

		// For a TCP connection (i.e. a server) we need a ServerSocket
		private ServerSocket in;


		private volatile boolean serverEnabled = false; 

		public synchronized void setEnabled(boolean state){
			serverEnabled = state;
		}

		public  synchronized boolean isEnabled(){
			return serverEnabled;
		}

		// In the constructor we try creating the server socket, on port 9000.
		public SingleThreadedServer() {
			try {
				// Beware: Only privileged users can use ports below 1023.
				in = new ServerSocket(9000);
			} catch (Exception e) {
				Log.e(TAG, "Cannot create socket. Due to: " + e.getMessage());
			}
		}

		private class ResponderThread extends Thread {
			Socket s; 
			ResponderThread(Socket s){
				this.s = s; 
			}
			@Override
			public void run() {
				// Get its associated OutputStream for writing.
				OutputStream responseStream = null;
				try {
					responseStream = s.getOutputStream();
				} catch (IOException e) {
					Log.e(TAG, "Cannot get outputstream.");
				}

				// Wrap it with a PrinStream for convenience.
				PrintStream writer = new PrintStream(responseStream);
				
				writer.print(greeting + "\n");
				/*
				try { 
					 Thread.sleep(30, 0);
					} 
					catch (InterruptedException e) {	}
				*/
				// Make sure data is sent and allocated resources are cleared.
				try {
					s.close();
				} catch (IOException e) {
					Log.e(TAG, "Error finishing request.");
				}

				Log.d(TAG, "Sent greeting.");
				// Continue the looping.
			}
		}


		@Override
		public void run() {

			// Always try serving incoming requests.
			while(isEnabled()) {

				//For every request we are allocated a new socket.
				Socket incomingRequest = null;
				Thread responder; 


				try {
					// Wait in blocked state for a request.
					incomingRequest = in.accept();
				} catch (IOException e) {
					Log.e(TAG, "Error when accepting connection.");
				}

				// When accept() returns a new request was received.
				// We use the incomingRequest socket for I/O
				Log.d(TAG, "New request from: " + incomingRequest.getInetAddress());

				ResponderThread t; 
				t = new ResponderThread(incomingRequest);
				t.start();

				/*
				// Get its associated OutputStream for writing.
				OutputStream responseStream = null;
				try {
					responseStream = incomingRequest.getOutputStream();
				} catch (IOException e) {
					Log.e(TAG, "Cannot get outputstream.");
				}

				// Wrap it with a PrinStream for convenience.
				PrintStream writer = new PrintStream(responseStream);
				try { Thread.sleep(300, 0);} 
				catch (InterruptedException e) {	}
				writer.print(greeting + "\n");

				// Make sure data is sent and allocated resources are cleared.
				try {
					incomingRequest.close();
				} catch (IOException e) {
					Log.e(TAG, "Error finishing request.");
				}

				Log.d(TAG, "Sent greeting.");
				// Continue the looping.
				 */


			}
			try {
				in.close();
			} catch (IOException e){ 
				Log.d(TAG, "problem closing");
			}
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		EditText greetingField = (EditText)findViewById(R.id.greeting);

		/**
		 * When the text changes, we change the greeting accordingly.
		 */
		greetingField.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				greeting = s.toString();
			}
		});

		if(greetingServer == null){
			greetingServer = new SingleThreadedServer();
			greetingThread = new Thread(greetingServer);
			greetingThread.start();
			greetingServer.setEnabled(true);
		}

		Button getGreeting = (Button)findViewById(R.id.get);
		getGreeting.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final String TAG = "ClientSocket";
				// The address of the server.
				final String friendId = ((EditText)findViewById(R.id.friend_identity)).getText().toString();
								 	 

				
				// Create then start the "worker" thread.
				new Thread(new Runnable() {
					@Override
					public void run() {

						final EditText responseGreetingField = (EditText)findViewById(R.id.response);
						String response = null;
						HttpClient client = new DefaultHttpClient();  
						//HttpGet get = new HttpGet("http://wifi.elcom.pub.ro/pdsd/expr_get.php?op=plus&t1=3&t2=11");
						HttpGet get = new HttpGet(friendId);
						
						//ResponseHandler<String> handler = new BasicResponseHandler();

						try {  
							HttpResponse response_get = client.execute(get);
							HttpEntity response_entity = response_get.getEntity();
							response = EntityUtils.toString(response_entity); 
							Log.i("RESPONSE", response);
						} 
						catch (ClientProtocolException e){
							e.printStackTrace();
						} catch (IOException e){
							e.printStackTrace();
						}

						// We need a final local variable in order to access it from an inner class.
						final String response_field = response;

						// Do the UI update.
						responseGreetingField.post(new Runnable() {
							@Override
							public void run() {
								if (response_field != null) {
									// Set the contents of the text field to the received response.
									responseGreetingField.setText("Got " + response_field);
								} else {
									Toast.makeText(MainActivity.this, "Your friend said nothing.", Toast.LENGTH_SHORT).show();
								}
							}	
						});
					}
				}).start();

			}	
		});

	}

	@Override
	protected void onDestroy() {
		
		Log.d(TAG, "onDestroy");
		
		greetingServer.setEnabled(false);
		
		super.onDestroy();
	}

}

