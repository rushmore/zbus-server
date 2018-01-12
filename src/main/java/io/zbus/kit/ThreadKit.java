package io.zbus.kit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ThreadKit { 
	public static class ManualResetEvent {
		private volatile CountDownLatch event;
		private static final Object mutex = new Object();

		public ManualResetEvent(boolean signalled) {
			if (signalled) {
				event = new CountDownLatch(0);
			} else {
				event = new CountDownLatch(1);
			}
		}

		public void set() {
			event.countDown();
		}

		public void reset() {
			synchronized (mutex) {
				if (event.getCount() == 0) {
					event = new CountDownLatch(1);
				}
			}
		}

		public void await() throws InterruptedException {
			event.await();
		}

		public boolean await(int timeout, TimeUnit unit) throws InterruptedException {
			return event.await(timeout, unit);
		}

		public boolean isSignalled() {
			return event.getCount() == 0;
		}
	}
}
