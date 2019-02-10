package it.azraelsec.Protocol;

public class ExecutionImpl implements Execution {
    Handler handler;
    public ExecutionImpl(Handler handler){
        this.handler = handler;
    }


    @Override
    public void run(Object[] args, Result result) {
        if (handler != null)
            handler.handle((String)args[0]);
    }
}
