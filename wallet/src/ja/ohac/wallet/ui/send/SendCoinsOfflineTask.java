/*
 * Copyright 2013-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ja.ohac.wallet.ui.send;

import java.math.BigInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import android.os.Handler;
import android.os.Looper;

import com.google.sha1coin.core.Coin;
import com.google.sha1coin.core.InsufficientMoneyException;
import com.google.sha1coin.core.Transaction;
import com.google.sha1coin.core.Wallet;
import com.google.sha1coin.core.Wallet.SendRequest;

/**
 * @author Andreas Schildbach
 */
public abstract class SendCoinsOfflineTask
{
	private final Wallet wallet;
	private final Handler backgroundHandler;
	private final Handler callbackHandler;

	public SendCoinsOfflineTask(@Nonnull final Wallet wallet, @Nonnull final Handler backgroundHandler)
	{
		this.wallet = wallet;
		this.backgroundHandler = backgroundHandler;
		this.callbackHandler = new Handler(Looper.myLooper());
	}

	public final void sendCoinsOffline(@Nonnull final SendRequest sendRequest)
	{
		backgroundHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final Transaction transaction = wallet.sendCoinsOffline(sendRequest); // can take long

					callbackHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							onSuccess(transaction);
						}
					});
				}
				catch (final InsufficientMoneyException x)
				{
					callbackHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							onInsufficientMoney(BigInteger.valueOf(x.missing.longValue()));
						}
					});
				}
				catch (final IllegalArgumentException x)
				{
					callbackHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							onFailure(x);
						}
					});
				}
			}
		});
	}

	protected abstract void onSuccess(@Nonnull Transaction transaction);

	protected void onInsufficientMoney(@Nullable BigInteger missing)
	{
		onFailure(new InsufficientMoneyException(Coin.valueOf(missing.longValue())));
	}

	protected abstract void onFailure(@Nonnull Exception exception);
}
