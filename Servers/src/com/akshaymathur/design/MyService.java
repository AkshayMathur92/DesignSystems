package com.akshaymathur.design;

import com.akshaymathur.design.tcpserver.Service;
import com.akshaymathur.design.tcpserver.Servable;

/**
 * This is a demo of creating a TCP Service.
 * Just create a class with public functions of input and output and annotate
 * with @code{Service}.
 * Then while building use flag javac -processor
 * com.akshaymathur.design.ServerAnnotationProcessor --processor-path ./bin
 * Then run your class suffixes with _Servable, in this particular instance run
 * java com.akshaymathur.design.MyService_Servable
 */
@Service
public class MyService {

    @Servable
    public String echo(String input) {
        return input;
    }

    @Servable
    public String sum(Integer a, Integer b) {
        return Integer.valueOf(a + b).toString();
    }

    @Servable
    public String helloworld() {
        return "Hello World!!";
    }

}
