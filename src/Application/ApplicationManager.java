package Application;

import java.util.ArrayList;

public class ApplicationManager {
    private static ArrayList<IApplication> application_list = new ArrayList<IApplication>();
    private static ApplicationManager instance = null;

    private ApplicationManager(){

    }

    public static ApplicationManager getInstance(){
        if(instance == null){
            instance = new ApplicationManager();
        }
        return instance;
    }

    public static void addApplication(IApplication app){application_list.add(app);}

    public IApplication getApplicationByPort(int port){
        for(int i = 0; i < application_list.size(); i++){
            IApplication app = application_list.get(i);
            if(app.getPort() == port){
                return app;
            }
        }
        return null;
    }

}
