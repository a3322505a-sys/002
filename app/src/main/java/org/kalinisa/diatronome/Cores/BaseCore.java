package org.kalinisa.diatronome.Cores;

import android.os.Handler;
import android.os.Message;

abstract public class BaseCore
{
  private volatile Handler m_handler;

  protected Handler getHandler() { return m_handler; }
  public void setHandler(Handler handler)
  {
    m_handler = handler;
  }

  protected void sendMessage (int what, int arg1, int arg2)
  {
    Handler handler = m_handler;
    if (handler != null)
    {
      Message msg = handler.obtainMessage();
      msg.what = what;
      msg.arg1 = arg1;
      msg.arg2 = arg2;
      handler.sendMessage(msg);
    }
  }

  protected void sendMessage (int what, Object obj)
  {
    Handler handler = m_handler;
    if (handler != null)
    {
      Message msg = handler.obtainMessage();
      msg.what = what;
      msg.obj = obj;
      handler.sendMessage(msg);
    }
  }
}
