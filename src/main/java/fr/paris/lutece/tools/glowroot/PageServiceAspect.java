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

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext.Priority;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class PageServiceAspect
{
    @Pointcut( className = "fr.paris.lutece.portal.service.page.PageService", methodName = "getPage", methodParameterTypes = {
            "java.lang.String", "int", "javax.servlet.http.HttpServletRequest" }, timerName = "Page" )
    public static class PageServiceAdvice
    {
        private static final TimerName timer = Agent.getTimerName( PageServiceAdvice.class );

        @OnBefore
        public static TraceEntry onBefore( OptionalThreadContext context, @BindParameter String strIdPage )
        {
            StringBuilder builder = new StringBuilder( "Page" );
            if ( LutecePluginProperties.includePageIDInTransactionName( ) )
            {
                builder.append( ' ' ).append( strIdPage );
                context.setTransactionName( builder.toString( ), Priority.USER_PLUGIN );
            } else
            {
                context.setTransactionName( "Page", Priority.USER_PLUGIN );
            }
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
}
