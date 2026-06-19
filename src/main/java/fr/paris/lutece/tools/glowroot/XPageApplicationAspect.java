/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.tools.glowroot;

import java.util.Enumeration;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext.Priority;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindParameterArray;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;

public class XPageApplicationAspect
{
    private static enum METHOD_TYPE
    {
        VIEW( "view" ), ACTION( "action" );

        public final String parameterName;
        public final String parameterNamePrefix;

        METHOD_TYPE( String paramName ) {
            parameterName = paramName;
            parameterNamePrefix = paramName + "_";
        }

        String getName( )
        {
            return parameterName;
        }

        String getPrefix( )
        {
            return parameterNamePrefix;
        }
    }

    @Shim( "javax.servlet.http.HttpServletRequest" )
    public interface HttpServletRequest
    {
        public String getParameter( String name );

        Enumeration<String> getParameterNames( );
    }

    @Pointcut( className = "fr.paris.lutece.portal.web.xpages.XPageApplication", methodName = "getPage", methodParameterTypes = {
            "javax.servlet.http.HttpServletRequest", "int",
            "fr.paris.lutece.portal.service.plugin.Plugin" }, timerName = "XPageApplication" )
    public static class XPageApplicationAdvice
    {
        private static final TimerName timer = Agent.getTimerName( XPageApplicationAdvice.class );

        @OnBefore
        public static TraceEntry onBefore( OptionalThreadContext context, @BindParameterArray Object[] parameters )
        {

            HttpServletRequest request = (HttpServletRequest) parameters[0];
            String xPage = request.getParameter( "page" );
            StringBuilder builder = new StringBuilder( "XPage " );
            builder.append( xPage );
            String view = getViewOrAction( request, METHOD_TYPE.VIEW );
            String action = getViewOrAction( request, METHOD_TYPE.ACTION );
            if ( view != null && !"".equals( view ) )
            {
                builder.append( " | view=" ).append( view );
            } else if ( action != null && !"".equals( action ) )
            {
                builder.append( " | action=" ).append( action );
            } else
            {
                builder.append( " | default" );
            }
            context.setTransactionName( builder.toString( ), Priority.USER_PLUGIN );
            return context.startTraceEntry( MessageSupplier.create( builder.toString( ) ), timer );
        }

        @OnReturn
        public static void onReturn( OptionalThreadContext context, @BindTraveler TraceEntry traceEntry )
        {
            traceEntry.end( );
        }

        @OnThrow
        public static void onThrow( @BindThrowable Throwable throwable, OptionalThreadContext context,
                @BindTraveler TraceEntry traceEntry )
        {
            traceEntry.endWithError( throwable );
        }
    }

    private static String getViewOrAction( HttpServletRequest request, METHOD_TYPE method )
    {
        String strRes = request.getParameter( method.getName( ) );

        if ( strRes != null )
        {
            return strRes;
        }

        Enumeration<String> parameters = request.getParameterNames( );

        while ( parameters.hasMoreElements( ) )
        {
            String strParameter = parameters.nextElement( );

            if ( strParameter.startsWith( method.getPrefix( ) ) )
            {
                return strParameter.substring( method.getPrefix( ).length( ) );
            }
        }

        return strRes;
    }
}
