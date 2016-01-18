package server;


import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
 
/**
 * Класс сервера
 */
public class Server {
   // public int COUNT_SOCKETS = 0;
    private ServerSocket ss; // сервер-сокет
    private Thread serverThread; // главная нить обработки сервер-сокета
    private final int PORT = 3033; // порт сервер сокета.
    //очередь, где храняться все SocketProcessorы для рассылки
    List<SocketProcessor> q = new ArrayList<SocketProcessor>();
    Iterator<SocketProcessor> iterator = q.iterator();
 
    /**
     * Конструктор объекта сервера
     */
    public Server() throws IOException {
        ss = new ServerSocket(PORT); // создаем сервер-сокет
    }
 
    /**
     * главный цикл прослушивания/ожидания коннекта.
     */
    void run() {
        
        serverThread = Thread.currentThread(); // со старта сохраняем нить (чтобы можно ее было interrupt())
        while (true) { //бесконечный цикл
            Socket s = getNewConn(); // получить новое соединение или фейк-соедиение
            if (s != null){ // "только если коннект успешно создан"...
                try {
                    final SocketProcessor processor = new SocketProcessor(s); // создаем сокет-процессор
                    final Thread thread = new Thread(processor); // создаем отдельную асинхронную нить чтения из сокета
                    thread.setDaemon(true); //ставим ее в демона (чтобы не ожидать ее закрытия)
                    thread.start(); //запускаем
                    q.add(processor);
              //      COUNT_SOCKETS++;
                   //добавляем в список активных сокет-процессоров
                } 
                
                catch (IOException ignored) {}  
            }
        }
    }
 
    /* 
          =========
    тут получаем коннект 
    */
    
    private Socket getNewConn() {
        Socket s = null;
        try {
            s = ss.accept();
        } catch (IOException e) {
            shutdownServer(); // если ошибка в момент приема - "гасим" сервер
        }
        return s;
    }
 
    /**
     * метод "глушения" сервера
     */
    private synchronized void shutdownServer() {
        // обрабатываем список рабочих коннектов, закрываем каждый
        while (iterator.hasNext()) {
           iterator.next().close();
        }
        
        if (!ss.isClosed()) {
            try {
                ss.close();
                System.exit(0);
            } catch (IOException ignored) {}
        }
    }
 
    /**
     * входная точка программы
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        new Server().run(); // если сервер не создался, программа
        // вылетит по эксепшену, и метод run() не запуститься
    }
 
    /**
     * вложенный класс асинхронной обработки одного коннекта.
     */
    private class SocketProcessor implements Runnable{
        Socket s; // наш сокет
        BufferedReader br; // буферизировнный читатель сокета
        BufferedWriter bw; // буферизированный писатель в сокет
 
        /**
         * Сохраняем сокет, пробуем создать читателя и писателя. Если не получается - вылетаем без создания объекта
         * @param socketParam сокет
         * @throws IOException Если ошибка в создании br || bw
         */
        SocketProcessor(Socket socketParam) throws IOException {
            s = socketParam;
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()) );
        }
 
        /**
         * Главный цикл чтения сообщений/рассылки
         */
        public void run() {
            while (!s.isClosed()) { // пока сокет не закрыт...
                String line = null;
                try {
                    line = br.readLine(); // пробуем прочесть.
                    System.out.println(line);
                } catch (IOException e) {
                    try {
                        s.close(); // если не получилось - закрываем сокет.
                    } catch (IOException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
 
               //рассылка по списку сокет-процессоров
                    
                /*    Iterator<SocketProcessor> iterator = q.iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next() != this) {
                        iterator.next().send(line);    
                        }
                        
                    }
                    */
                    for (SocketProcessor sp:q) {
                        /*if (line == "SocketOut") {
                            if (sp == this) {
                                sp.close();
                                COUNT_SOCKETS--;
                                if (COUNT_SOCKETS == 0) {
                                    shutdownServer();
                                }
                            }
                        }
                        else */
                      
                        
                            sp.send(line);
                   
                        
                       /* else {
                            String linePlus = "(you)"+line;
                            sp.send(linePlus);
                        }*/
                
                    }
                    
            
            }
        }
 
        /* ===============================
        
        
        
        В клиенте дописать на отправку в начало строки "name: "
        Для реалистичности
        
        
        ~    готово )
         
        
        */
        
        public synchronized void send(String line) {
            try {
                bw.write(line); // пишем строку
                bw.write("\n"); // пишем перевод строки
                bw.flush(); // отправляем
            } catch (IOException e) {
                close(); //если глюк в момент отправки - закрываем данный сокет.
            }
        }
 
        /**
         * метод аккуратно закрывает сокет и убирает его со списка активных сокетов
         */
        public synchronized void close() {
            q.remove(this); //убираем из списка
            if (!s.isClosed()) {
                try {
                    s.close(); // закрываем
                } catch (IOException ignored) {}
            }
        }
 
        /**
         * финализатор просто на всякий случай.
         * @throws Throwable
         */
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            close();
        }
    }
}




/*public class server {

    static ServerSocket serverSocket = null;
    static Socket clientSocket = null;
    static List<ClientThread> t = new ArrayList();
    public static final int PORT = 8080;
    public static final int SRV_MAX = 2;

    public static void main(String[] args) throws IOException {
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException ex) {
            System.out.println("Can't open port " + PORT);
        }
        int iter = 0;
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                ClientThread client = new ClientThread(clientSocket, t);
                t.add(client);
                
                client.start();
            iter++;
            System.out.println(iter+" Client succesfully connected to server");
            } catch (IOException ex) {
                System.out.println("Couldn't connect client to " + PORT);
            }
            
        }
    }
}

class ClientThread extends Thread {

    BufferedReader in = null;
    PrintStream out = null;
    Socket clientSocket = null;
    List<ClientThread> t;
    String name;

    public ClientThread(Socket clientSocket, List<ClientThread> t) {
        this.clientSocket = clientSocket;
        this.t = t;
    }

    public void run() {
        String line;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintStream(clientSocket.getOutputStream());
            out.println("Enter your name: ");
            name = in.readLine();
            out.println("Welcome " + name + " type /q to leave chat");
            for (ClientThread client : t) {
                client.out.println("User " + name + " is online now!");
            }

            while (true) {
                line = in.readLine();
                if (line.startsWith("/q")) {
                    break;
                }

                if (line.startsWith("<")) {
                    String tempName = line.substring(1, line.lastIndexOf(">"));
                    System.out.println(tempName);
                    boolean flag = true;
                    for (ClientThread ct : t) {
                        if (ct != null) {
                            if (ct.name.equals(tempName)) {
                                ct.out.println(line);
                                flag = false;
                            }
                        } else {
                            break;
                        }
                    }
                    if (flag) {
                        this.out.println("User " + tempName + " is not online now.");
                    }

                } else {
                    for (ClientThread ct : t) {
                        ct.out.println("<" + name + ">" + line);
                    }

                }
            }
            for (ClientThread ct : t) {
                if (ct != null && ct != this) {
                    ct.out.println("The user " + name + " is leaving chat");
                }
            }

            out.println("Bye" + name);
            for (ClientThread ct : t) {
                if (ct == this) {
                    ct = null;
                }
            }

            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException ex) {
            System.out.println("Disconnect");
        }
    }

}
*/
/*class thread_server extends Thread {
 private Socket socket;
 private Socket target;
 private BufferedReader in;
 private PrintWriter out;
    
 public thread_server(Socket s1, Socket s2) throws IOException {
 socket = s1;
 target = s2;
 in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
 out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(target.getOutputStream())), true);
 run(target);
 }
 public void run(Socket target) {
 try {
 while (true) {
 String str = in.readLine();
 if (str.equals("END")) {
 break;
 }
 System.out.println("Echoing: "+str);
 out.println(str);
 } 
 System.out.println("Closing");
 }
 catch (IOException ex) {
 System.err.println("IO Exception");
 }
 finally {
 try {
 socket.close();
 }
 catch (IOException ex) {
 System.out.println("Error closing the connection");
 }
 }
 }
    
 }
 public class server {
 public static final int PORT = 8080;
 public static final int SRV_MAX = 2;
    
 public static void main(String[] args) throws IOException {
 ServerSocket s = new ServerSocket(PORT);
 Socket socket1 = null;
 Socket socket2 = null;
 System.out.println("Server started");
 try {
 socket1 = s.accept();
 socket2 = s.accept();
 try {
 new thread_server(socket1, socket2);
 new thread_server(socket2, socket1);
 } catch (IOException ex) {
 socket1.close();
 socket2.close();
 }
 }
        
 finally {
 s.close();
 }
 }
 }
 */
/* public static final int PORT = 8080;
 public static void main(String[] args) throws IOException {
 BufferedReader in = null;
 PrintWriter out = null;
 ServerSocket ss = null;
 Socket s[] = null;
 System.out.println("Server is ready");
 try {
 ss = new ServerSocket(PORT);
 } catch (IOException ex) {
 System.out.println("Couldn't listen to port "+PORT);
 System.exit(-1);
 }
 try {
 for (int i =0; i<2; i++) {
 System.out.println("Waiting for "+(i+1)+" client");
 s[i] = ss.accept();
 System.out.println("Client "+(i+1)+" connected");
 }
 } catch(IOException ex){
 System.out.println("Can't connect to the client's");
 System.exit(-1);
 }
 }
 }

 */
